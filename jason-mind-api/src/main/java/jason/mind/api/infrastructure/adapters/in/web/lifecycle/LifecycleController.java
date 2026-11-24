package jason.mind.api.infrastructure.adapters.in.web.lifecycle;

import jason.mind.api.core.ports.in.lifecycle.FindEntityLifecycleIn;
import jason.mind.api.core.ports.in.lifecycle.FindEntityLifecyclePort;
import jason.mind.api.infrastructure.adapters.in.web.WebExchange;
import lombok.RequiredArgsConstructor;
import net.peelweb.context.controller.Controller;
import net.peelweb.context.controller.Mapping;
import net.peelweb.context.endpoint.Request;
import net.peelweb.context.endpoint.Response;
import net.peelweb.context.endpoint.Responses;

/**
 * Controller that exposes the lifecycle of an entity of an agent.
 * Returns the paginated occurrences that make up the history of the entity.
 *
 * @author Gabriel Lima
 */
@Controller("/api/agents")
@RequiredArgsConstructor
public class LifecycleController {

    /** Input port that finds the lifecycle of an entity. */
    private final FindEntityLifecyclePort findEntityLifecycle;

    /**
     * Returns the lifecycle of the entity whose agent and id are given in the path, paginated by the
     * {@code after} and {@code limit} parameters.
     *
     * @param request HTTP request carrying the agent name, the entity id and the pagination parameters
     * @return response with the lifecycle of the entity
     */
    @Mapping("/{name}/entities/{id}/lifecycle")
    public Response getLifecycle(Request request) {
        return WebExchange.handle("lifecycle", () -> Responses.ok(this.findEntityLifecycle.execute(
                new FindEntityLifecycleIn(request.getPathVariable("name"), request.getPathVariable("id"),
                        WebExchange.longValue(request, "after"), WebExchange.integer(request, "limit")))));
    }
}
