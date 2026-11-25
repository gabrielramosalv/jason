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
import net.peelweb.enums.HttpMethod;
import net.peelweb.enums.HttpResponseCode;
import net.peelweb.enums.HttpContentType;

/**
 * Controller of the manual prune of every agent. The request is only registered and answered with 202; each agent
 * applies it at the end of its next cycle.
 *
 * @author Gabriel Lima
 */
@Controller("/api/prune")
@RequiredArgsConstructor
public class PruneController {

    /** Input port that registers the prune. */
    private final RequestPrunePort requestPrune;

    /**
     * Asks for the history of every agent to be pruned up to the cycle in the {@code until} parameter.
     *
     * @param request HTTP request carrying {@code until}
     * @return 202 with what was requested for each agent, or bad request when {@code until} is missing or invalid
     */
    @Mapping(method = HttpMethod.POST)
    public Response prune(Request request) {
        return WebExchange.write("prune", request, () -> Responses.mount(HttpResponseCode.ACCEPTED,
                this.requestPrune.execute(new RequestPruneIn(null, WebExchange.integer(request, "until"))),
                HttpContentType.JSON));
    }
}
