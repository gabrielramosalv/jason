package jason.mind.api.infrastructure.adapters.in.web.settings;

import jason.mind.api.core.ports.in.settings.FindSettingsPort;
import jason.mind.api.core.ports.in.settings.UpdateSettingsIn;
import jason.mind.api.core.ports.in.settings.UpdateSettingsPort;
import jason.mind.api.infrastructure.adapters.in.web.WebExchange;
import net.peelweb.context.controller.Controller;
import net.peelweb.context.controller.Mapping;
import net.peelweb.context.endpoint.Request;
import net.peelweb.context.endpoint.Response;
import net.peelweb.context.endpoint.Responses;
import net.peelweb.enums.HttpMethod;

@Controller("/api/settings")
public class SettingsController {

    private final FindSettingsPort findSettings;
    private final UpdateSettingsPort updateSettings;

    public SettingsController(FindSettingsPort findSettings, UpdateSettingsPort updateSettings) {
        this.findSettings = findSettings;
        this.updateSettings = updateSettings;
    }

    @Mapping
    public Response find() {
        return WebExchange.handle(() -> Responses.ok(findSettings.execute()));
    }

    @Mapping(method = HttpMethod.PUT)
    public Response update(Request request) {
        return WebExchange.handle(() -> {
            SettingsRequest body;
            try {
                body = request.getBodyAs(SettingsRequest.class);
            } catch (RuntimeException e) {
                throw new WebExchange.BadParameterException("invalid body: expected {\"retentionCycles\": n}");
            }
            if (body == null || body.retentionCycles == null)
                throw new WebExchange.BadParameterException("invalid body: expected {\"retentionCycles\": n}");
            return Responses.ok(updateSettings.execute(new UpdateSettingsIn(body.retentionCycles)));
        });
    }

    public static class SettingsRequest {
        public Long retentionCycles;

        public SettingsRequest() {
        }
    }
}
