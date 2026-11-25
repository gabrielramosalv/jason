package jason.mind.api.infrastructure.adapters.in.web;

import net.peelweb.context.endpoint.Request;
import net.peelweb.context.endpoint.Response;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests the origin rules of the writes (D-155): local {@code Host}, same-origin {@code Origin} and JSON body.
 */
class WriteOriginGuardTest {

    private static final Map<String, String> VALID = Map.of(
            "host", "127.0.0.1:3272",
            "content-type", "application/json");

    private static Request request(Map<String, String> headers) {
        return (Request) Proxy.newProxyInstance(Request.class.getClassLoader(), new Class<?>[] {Request.class},
                (proxy, method, args) -> method.getName().equals("getHeader")
                        ? headers.get(((String) args[0]).toLowerCase())
                        : null);
    }

    private static Map<String, String> with(String key, String value) {
        Map<String, String> copy = new java.util.HashMap<>(VALID);
        copy.put(key, value);
        return copy;
    }

    private static int status(Response response) {
        return response.getResponseCode().getNumber();
    }

    @Test
    void jsonWriteFromTheLocalHostPasses() {
        assertNull(WebExchange.refuseWrite(request(VALID)));
    }

    @Test
    void sameOriginIsAccepted() {
        assertNull(WebExchange.refuseWrite(request(with("origin", "http://127.0.0.1:3272"))));
    }

    @Test
    void anotherOriginIsRefusedWithForbidden() {
        assertEquals(403, status(WebExchange.refuseWrite(request(with("origin", "http://evil.example")))));
    }

    @Test
    void aNonLocalHostIsRefusedAgainstDnsRebinding() {
        assertEquals(403, status(WebExchange.refuseWrite(request(with("host", "evil.example:3272")))));
    }

    @Test
    void localNamesAreAllowedWithAnyPort() {
        assertNull(WebExchange.refuseWrite(request(with("host", "localhost:8080"))));
        assertNull(WebExchange.refuseWrite(request(with("host", "[::1]:3272"))));
    }

    @Test
    void aSimpleContentTypeTurnsIntoUnsupportedMediaType() {
        assertEquals(415, status(WebExchange.refuseWrite(request(with("content-type", "text/plain")))));
        assertEquals(415, status(WebExchange.refuseWrite(request(Map.of("host", "127.0.0.1:3272")))));
    }

    @Test
    void hostIsCheckedBeforeOriginAndOriginBeforeContentType() {
        Map<String, String> bad = Map.of("host", "evil.example", "origin", "http://x", "content-type", "text/plain");
        assertEquals(403, status(WebExchange.refuseWrite(request(bad))));
    }
}
