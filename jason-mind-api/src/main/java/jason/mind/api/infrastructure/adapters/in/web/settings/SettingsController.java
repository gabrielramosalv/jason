package jason.mind.api.infrastructure.adapters.in.web.settings;

import jason.mind.api.core.ports.in.settings.FindSettingsPort;
import jason.mind.api.core.ports.in.settings.UpdateSettingsIn;
import jason.mind.api.core.ports.in.settings.UpdateSettingsPort;
import jason.mind.api.infrastructure.adapters.in.web.WebExchange;
import lombok.RequiredArgsConstructor;
import net.peelweb.context.controller.Controller;
import net.peelweb.context.controller.Mapping;
import net.peelweb.context.endpoint.Request;
import net.peelweb.context.endpoint.Response;
import net.peelweb.context.endpoint.Responses;
import net.peelweb.enums.HttpMethod;

/**
 * Controller that exposes the settings of the Mind API.
 * Reads the current settings and updates the retention of cycles.
 *
 * @author Gabriel Lima
 */
@Controller("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    /** Input port that finds the current settings. */
    private final FindSettingsPort findSettings;

    /** Input port that updates the settings. */
    private final UpdateSettingsPort updateSettings;

    /**
     * Returns the current settings.
     *
     * @return response with the current settings
     */
    @Mapping
    public Response find() {
        return WebExchange.handle("settings", () -> Responses.ok(this.findSettings.execute()));
    }

    /**
     * Updates the retention of cycles with the value in the request body.
     *
     * @param request HTTP request whose body holds the {@code retentionCycles} value
     * @return response with the updated settings, or bad request when the body is missing or invalid
     */
    @Mapping(method = HttpMethod.PUT)
    public Response update(Request request) {
        return WebExchange.handle("settingsUpdate", () -> {
            SettingsRequest body;
            try {
                body = request.getBodyAs(SettingsRequest.class);
            } catch (RuntimeException e) {
                throw new WebExchange.BadParameterException("invalid body: expected {\"retentionCycles\": n}");
            }
            if (body == null || body.retentionCycles == null && body.memoryBudgetBytes == null) {
                throw new WebExchange.BadParameterException(
                        "invalid body: expected {\"retentionCycles\": n} or {\"memoryBudgetBytes\": n}");
            }
            return Responses.ok(this.updateSettings.execute(
                    new UpdateSettingsIn(body.retentionCycles, body.memoryBudgetBytes)));
        });
    }

    /**
     * Request body of the settings update.
     * Holds the new retention of cycles in a mutable public field filled by deserialization.
     */
    public static class SettingsRequest {

        /** Number of cycles to retain in the history. */
        public Long retentionCycles;

        /** Bytes the history of all agents together may use. */
        public Long memoryBudgetBytes;

        /**
         * Creates an empty request body to be filled by deserialization.
         */
        public SettingsRequest() {
        }
    }
}
