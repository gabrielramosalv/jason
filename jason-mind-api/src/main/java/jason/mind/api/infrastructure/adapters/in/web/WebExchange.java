package jason.mind.api.infrastructure.adapters.in.web;

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

public final class WebExchange {

    private static final Logger LOGGER = Logger.getLogger("Mind");

    private WebExchange() {
    }

    public static final class BadParameterException extends RuntimeException {
        public BadParameterException(String message) {
            super(message);
        }
    }

    public static Response handle(Supplier<Response> route) {
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

    public static Integer integer(Request request, String name) {
        String v = request.getParameter(name);
        if (v == null || v.isEmpty()) return null;
        try {
            return Integer.valueOf(v.trim());
        } catch (NumberFormatException e) {
            throw new BadParameterException(name + " must be an integer: " + v);
        }
    }

    public static Long longValue(Request request, String name) {
        String v = request.getParameter(name);
        if (v == null || v.isEmpty()) return null;
        try {
            return Long.valueOf(v.trim());
        } catch (NumberFormatException e) {
            throw new BadParameterException(name + " must be an integer: " + v);
        }
    }

    public static int pathInteger(Request request, String name) {
        String v = request.getPathVariable(name);
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            throw new BadParameterException(name + " must be an integer: " + v);
        }
    }

    public static Set<String> set(Request request, String name) {
        String v = request.getParameter(name);
        if (v == null) return null;
        Set<String> r = new LinkedHashSet<>();
        Arrays.stream(v.split(",")).map(String::trim).filter(s -> !s.isEmpty()).forEach(r::add);
        return r;
    }

    public static Response withETag(Request request, String etag, Supplier<Object> body) {
        String quoted = "\"" + etag + "\"";
        String inm = request.getHeader("If-None-Match");
        if (inm != null && matches(inm, quoted))
            return Responses.notModified().header("ETag", quoted);
        return Responses.ok(body.get()).header("ETag", quoted);
    }

    static boolean matches(String ifNoneMatch, String quoted) {
        int i = 0, n = ifNoneMatch.length();
        while (i < n) {
            char c = ifNoneMatch.charAt(i);
            if (c == ',' || Character.isWhitespace(c)) { i++; continue; }
            if (c == '*') return true;
            if (ifNoneMatch.startsWith("W/", i)) i += 2;
            if (i >= n || ifNoneMatch.charAt(i) != '"') return false;
            int close = ifNoneMatch.indexOf('"', i + 1);
            if (close < 0) return false;
            if (ifNoneMatch.substring(i, close + 1).equals(quoted)) return true;
            i = close + 1;
        }
        return false;
    }
}
