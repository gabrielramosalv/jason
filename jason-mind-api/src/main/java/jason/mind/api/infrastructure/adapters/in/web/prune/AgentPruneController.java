package jason.mind.api.infrastructure.adapters.in.web.prune;

import jason.mind.api.core.ports.in.prune.RequestPruneIn;
import jason.mind.api.core.ports.in.prune.RequestPrunePort;
import jason.mind.api.infrastructure.adapters.in.web.WebExchange;
import lombok.RequiredArgsConstructor;
import net.peelweb.context.controller.Controller;
import net.peelweb.context.controller.Mapping;
import net.peelweb.context.endpoint.Request;
import net.peelweb.context.endpoint.Response;
import net.peelweb.context.endpoint.Responses;
import net.peelweb.enums.HttpContentType;
import net.peelweb.enums.HttpMethod;
import net.peelweb.enums.HttpResponseCode;

/**
 * Controller of the manual prune of one agent, under the agents prefix. The request is only registered and answered
 * with 202; the agent applies it at the end of its next cycle.
 *
 * @author Gabriel Lima
 */
@Controller("/api/agents")
@RequiredArgsConstructor
public class AgentPruneController {

    /** Input port that registers the prune. */
    private final RequestPrunePort requestPrune;

    /**
     * Asks for the history of the agent named in the path to be pruned up to the cycle in {@code until}.
     *
     * @param request HTTP request carrying the agent name and {@code until}
     * @return 202 with what was requested, 404 for an unknown agent, or 400 when {@code until} is missing or invalid
     */
    @Mapping(value = "/{name}/prune", method = HttpMethod.POST)
    public Response prune(Request request) {
        return WebExchange.write("agentPrune", request, () -> Responses.mount(HttpResponseCode.ACCEPTED,
                this.requestPrune.execute(
                        new RequestPruneIn(request.getPathVariable("name"), WebExchange.integer(request, "until"))),
                HttpContentType.JSON));
    }
}
