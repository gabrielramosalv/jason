package jason.mind.api.infrastructure.adapters.in.web;

import jason.mind.api.infrastructure.metrics.MindTiming;
import jason.mind.api.core.application.history.AgentNotFoundException;
import jason.mind.api.core.application.history.CycleNotFoundException;
import jason.mind.api.core.application.history.CyclePrunedException;
import jason.mind.api.core.application.history.EntityNotFoundException;
import jason.mind.api.core.application.history.EntityPrunedException;
import jason.mind.api.core.application.history.InvalidQueryException;
import jason.mind.api.core.application.settings.InvalidSettingsException;
import net.peelweb.context.endpoint.Request;
import net.peelweb.context.endpoint.Response;
import net.peelweb.context.endpoint.Responses;
import net.peelweb.enums.HttpResponseCode;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Helper that holds the HTTP plumbing shared by the web controllers.
 * Translates exceptions into HTTP responses, parses request parameters and answers conditional requests by ETag.
 *
 * @author Gabriel Lima
 */
public final class WebExchange {

    /** Logger for unexpected route errors. */
    private static final Logger LOGGER = Logger.getLogger("Mind");

    /**
     * Prevents instantiation of this static helper.
     */
    private WebExchange() {
    }

        /**
     * Runs a route, timing its use case when the benchmark timing is on, and turns the known exceptions of the use
     * cases into error responses.
     *
     * @param name  name of the route, used to attribute the timing
     * @param route code of the route
     * @return response of the route, or the error response of the exception it threw
     */
    public static Response handle(String name, Supplier<Response> route) {
        return MindTiming.timeRoute(name, () -> handle(route));
    }

/**
     * Runs a write route after checking where the request comes from (D-155): the {@code Host} must be a local
     * name, an {@code Origin} must be the server itself, and the body must be declared as JSON. The last rule
     * turns even the empty POST of a prune into a request a browser has to preflight, which Peel Web denies.
     *
     * @param name    name of the route, used to attribute the timing
     * @param request HTTP request of the write
     * @param route   code of the route
     * @return forbidden or unsupported media type when the request is refused, otherwise the response of the route
     */
    public static Response write(String name, Request request, Supplier<Response> route) {
        Response refused = refuseWrite(request);
        return refused != null ? refused : handle(name, route);
    }

    /**
     * Applies the origin rules of the writes.
     *
     * @param request HTTP request of the write
     * @return the refusal response, or {@code null} when the write may proceed
     */
    static Response refuseWrite(Request request) {
        String host = request.getHeader("Host");
        if (!allowedHost(host)) {
            return Responses.error(HttpResponseCode.FORBIDDEN, "host not allowed");
        }
        String origin = request.getHeader("Origin");
        if (origin != null && !origin.equals("http://" + host) && !origin.equals("https://" + host)) {
            return Responses.error(HttpResponseCode.FORBIDDEN, "origin not allowed");
        }
        String type = request.getHeader("Content-Type");
        if (type == null || !type.toLowerCase(Locale.ROOT).startsWith("application/json")) {
            return Responses.error(HttpResponseCode.UNSUPPORTED_MEDIA_TYPE, "content type must be application/json");
        }
        return null;
    }

    /**
     * Tells whether a {@code Host} header names this machine: {@code localhost}, {@code 127.0.0.1}, {@code [::1]}
     * or the address the server was told to listen on ({@code mind.host}), with any port.
     *
     * @param host value of the {@code Host} header, may be {@code null}
     * @return {@code true} when the name is allowed
     */
    static boolean allowedHost(String host) {
        if (host == null || host.isBlank()) {
            return false;
        }
        String name = host.startsWith("[") ? host.substring(0, host.indexOf(']') + 1)
                : host.contains(":") ? host.substring(0, host.indexOf(':')) : host;
        name = name.toLowerCase(Locale.ROOT);
        String configured = System.getProperty("mind.host");
        return name.equals("localhost") || name.equals("127.0.0.1") || name.equals("[::1]")
                || configured != null && !configured.equals("0.0.0.0") && name.equalsIgnoreCase(configured);
    }

    /**
     * Runs a route and translates the known exceptions into HTTP responses: not found, gone and bad request.
     * Any other runtime exception is logged and answered with an internal server error.
     *
     * @param route route that produces the response
     * @return response of the route, or the error response matching the exception it threw
     */
    private static Response handle(Supplier<Response> route) {
        try {
            return route.get();
        } catch (AgentNotFoundException e) {
            return Responses.notFound();
        } catch (CycleNotFoundException | EntityNotFoundException e) {
            return Responses.notFound(e.getMessage());
        } catch (CyclePrunedException | EntityPrunedException e) {
            return Responses.gone(e.getMessage());
        } catch (InvalidQueryException | InvalidSettingsException | BadParameterException e) {
            return Responses.badRequest(e.getMessage());
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Mind: route error", e);
            return Responses.internalServerError();
        }
    }

    /**
     * Reads a query parameter as an integer.
     *
     * @param request HTTP request holding the parameter
     * @param name    name of the query parameter
     * @return value of the parameter, or {@code null} when it is absent or empty
     * @throws BadParameterException when the value is not an integer
     */
    public static Integer integer(Request request, String name) {
        String v = request.getParameter(name);
        if (v == null || v.isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(v.trim());
        } catch (NumberFormatException e) {
            throw new BadParameterException(name + " must be an integer: " + v);
        }
    }

    /**
     * Reads a query parameter as a long.
     *
     * @param request HTTP request holding the parameter
     * @param name    name of the query parameter
     * @return value of the parameter, or {@code null} when it is absent or empty
     * @throws BadParameterException when the value is not an integer
     */
    public static Long longValue(Request request, String name) {
        String v = request.getParameter(name);
        if (v == null || v.isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(v.trim());
        } catch (NumberFormatException e) {
            throw new BadParameterException(name + " must be an integer: " + v);
        }
    }

    /**
     * Reads a path variable as an integer.
     *
     * @param request HTTP request holding the path variable
     * @param name    name of the path variable
     * @return value of the path variable
     * @throws BadParameterException when the value is not an integer
     */
    public static int pathInteger(Request request, String name) {
        String v = request.getPathVariable(name);
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            throw new BadParameterException(name + " must be an integer: " + v);
        }
    }

    /**
     * Reads a comma-separated query parameter as a set, trimming the items and dropping the empty ones.
     *
     * @param request HTTP request holding the parameter
     * @param name    name of the query parameter
     * @return items of the parameter in their original order, or {@code null} when it is absent
     */
    public static Set<String> set(Request request, String name) {
        String v = request.getParameter(name);
        if (v == null) {
            return null;
        }
        Set<String> r = new LinkedHashSet<>();
        Arrays.stream(v.split(",")).map(String::trim).filter(s -> !s.isEmpty()).forEach(r::add);
        return r;
    }

    /**
     * Identity of this run of the MAS, prefixed to every ETag. Without it two runs produce the same tag for the same
     * cycle ({@code 7:intentions}), and a browser holding the previous run answers 304 with the old body.
     */
    private static final String RUN_ID = Long.toHexString(System.currentTimeMillis());

    /**
     * Answers a request with an ETag, returning not modified when the {@code If-None-Match} header matches it.
     * The body is only built when the response is not a not modified one.
     *
     * @param request HTTP request holding the conditional header
     * @param etag    unquoted entity tag of the current representation
     * @param body    supplier of the response body
     * @return ok response with the body, or not modified, both carrying the ETag header
     */
    public static Response withETag(Request request, String etag, Supplier<Object> body) {
        String quoted = "\"" + RUN_ID + "-" + etag + "\"";
        String inm = request.getHeader("If-None-Match");
        if (inm != null && matches(inm, quoted)) {
            return Responses.notModified().header("ETag", quoted);
        }
        return Responses.ok(body.get()).header("ETag", quoted);
    }

    /**
     * Checks whether an {@code If-None-Match} header value matches an entity tag, using weak comparison.
     *
     * @param ifNoneMatch value of the {@code If-None-Match} header
     * @param quoted      quoted entity tag of the current representation
     * @return {@code true} when the header is {@code *} or lists the entity tag, weak or strong
     */
    static boolean matches(String ifNoneMatch, String quoted) {
        int i = 0, n = ifNoneMatch.length();
        while (i < n) {
            char c = ifNoneMatch.charAt(i);
            if (c == ',' || Character.isWhitespace(c)) {
                i++;
                continue;
            }
            if (c == '*') {
                return true;
            }
            if (ifNoneMatch.startsWith("W/", i)) {
                i += 2;
            }
            if (i >= n || ifNoneMatch.charAt(i) != '"') {
                return false;
            }
            int close = ifNoneMatch.indexOf('"', i + 1);
            if (close < 0) {
                return false;
            }
            if (ifNoneMatch.substring(i, close + 1).equals(quoted)) {
                return true;
            }
            i = close + 1;
        }
        return false;
    }

    /**
     * Exception thrown when a request parameter or body is missing or malformed.
     * Translated into a bad request response.
     */
    public static final class BadParameterException extends RuntimeException {

        /**
         * Creates the exception with the message describing the invalid parameter.
         *
         * @param message description of the invalid parameter, returned in the response body
         */
        public BadParameterException(String message) {
            super(message);
        }
    }
}
