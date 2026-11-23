package jason.mind.api.infrastructure.adapters.in.web.lifecycle;

import jason.mind.api.core.ports.in.lifecycle.FindEntityLifecycleIn;
import jason.mind.api.core.ports.in.lifecycle.FindEntityLifecyclePort;
import jason.mind.api.infrastructure.adapters.in.web.WebExchange;
import net.peelweb.context.controller.Controller;
import net.peelweb.context.controller.Mapping;
import net.peelweb.context.endpoint.Request;
import net.peelweb.context.endpoint.Response;
import net.peelweb.context.endpoint.Responses;

@Controller("/api/agents")
public class LifecycleController {

    private final FindEntityLifecyclePort findEntityLifecycle;

    public LifecycleController(FindEntityLifecyclePort findEntityLifecycle) {
        this.findEntityLifecycle = findEntityLifecycle;
    }

    @Mapping("/{name}/entities/{id}/lifecycle")
    public Response lifecycle(Request request) {
        return WebExchange.handle(() -> Responses.ok(findEntityLifecycle.execute(new FindEntityLifecycleIn(
                request.getPathVariable("name"), request.getPathVariable("id"),
                WebExchange.longValue(request, "after"), WebExchange.integer(request, "limit")))));
    }
}
