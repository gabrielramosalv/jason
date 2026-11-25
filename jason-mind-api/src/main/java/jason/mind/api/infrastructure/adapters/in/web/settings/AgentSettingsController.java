package jason.mind.api.infrastructure.adapters.in.web.settings;

import jason.mind.api.core.ports.in.settings.UpdateAgentSettingsIn;
import jason.mind.api.core.ports.in.settings.UpdateAgentSettingsPort;
import jason.mind.api.infrastructure.adapters.in.web.WebExchange;
import lombok.RequiredArgsConstructor;
import net.peelweb.context.controller.Controller;
import net.peelweb.context.controller.Mapping;
import net.peelweb.context.endpoint.Request;
import net.peelweb.context.endpoint.Response;
import net.peelweb.context.endpoint.Responses;
import net.peelweb.enums.HttpMethod;

/**
 * Controller of the retention of one agent, under the agents prefix. Like the system settings, it accepts any origin
 * for now [REQ-035].
 *
 * @author Gabriel Lima
 */
@Controller("/api/agents")
@RequiredArgsConstructor
public class AgentSettingsController {

    /** Input port that sets or removes the retention of an agent. */
    private final UpdateAgentSettingsPort updateAgentSettings;

    /**
     * Sets the retention of the agent named in the path to the value in the request body.
     *
     * @param request HTTP request whose body holds {@code retentionCycles}
     * @return response with the settings in force, bad request when the body is missing or invalid, or not found
     */
    @Mapping(value = "/{name}/settings", method = HttpMethod.PUT)
    public Response update(Request request) {
        return WebExchange.write("agentSettingsUpdate", request, () -> {
            AgentSettingsRequest body;
            try {
                body = request.getBodyAs(AgentSettingsRequest.class);
            } catch (RuntimeException e) {
                throw new WebExchange.BadParameterException("invalid body: expected {\"retentionCycles\": n}");
            }
            if (body == null || body.retentionCycles == null) {
                throw new WebExchange.BadParameterException("invalid body: expected {\"retentionCycles\": n}");
            }
            return Responses.ok(this.updateAgentSettings.execute(
                    new UpdateAgentSettingsIn(request.getPathVariable("name"), body.retentionCycles)));
        });
    }

    /**
     * Removes the retention of the agent named in the path, so that it goes back to the default of the system.
     *
     * @param request HTTP request carrying the agent name
     * @return response with the settings in force, or not found for an unknown agent
     */
    @Mapping(value = "/{name}/settings", method = HttpMethod.DELETE)
    public Response reset(Request request) {
        return WebExchange.write("agentSettingsReset", request, () -> Responses.ok(
                this.updateAgentSettings.execute(new UpdateAgentSettingsIn(request.getPathVariable("name"), null))));
    }

    /**
     * Request body of the change of the retention of an agent.
     */
    public static class AgentSettingsRequest {

        /** Number of cycles the agent retains. */
        public Long retentionCycles;

        /**
         * Creates an empty request body to be filled by deserialization.
         */
        public AgentSettingsRequest() {
        }
    }
}
