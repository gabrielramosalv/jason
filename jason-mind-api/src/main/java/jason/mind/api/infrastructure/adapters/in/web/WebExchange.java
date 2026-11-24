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

import java.util.Arrays;
import java.util.LinkedHashSet;
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
     * Answers a request with an ETag, returning not modified when the {@code If-None-Match} header matches it.
     * The body is only built when the response is not a not modified one.
     *
     * @param request HTTP request holding the conditional header
     * @param etag    unquoted entity tag of the current representation
     * @param body    supplier of the response body
     * @return ok response with the body, or not modified, both carrying the ETag header
     */
    public static Response withETag(Request request, String etag, Supplier<Object> body) {
        String quoted = "\"" + etag + "\"";
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
