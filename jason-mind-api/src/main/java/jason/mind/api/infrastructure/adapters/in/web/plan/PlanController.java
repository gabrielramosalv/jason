package jason.mind.api.infrastructure.adapters.in.web.plan;

import jason.mind.api.core.ports.in.plan.FindPlansIn;
import jason.mind.api.core.ports.in.plan.FindPlansPort;
import jason.mind.api.core.ports.in.plan.PlansOut;
import jason.mind.api.infrastructure.adapters.in.web.WebExchange;
import net.peelweb.context.controller.Controller;
import net.peelweb.context.controller.Mapping;
import net.peelweb.context.endpoint.Request;
import net.peelweb.context.endpoint.Response;

@Controller("/api/agents")
public class PlanController {

    private final FindPlansPort findPlans;

    public PlanController(FindPlansPort findPlans) {
        this.findPlans = findPlans;
    }

    @Mapping("/{name}/plans")
    public Response plans(Request request) {
        return WebExchange.handle(() -> {
            PlansOut out = findPlans.execute(new FindPlansIn(request.getPathVariable("name")));
            return WebExchange.withETag(request, "plans:" + out.version(), () -> out);
        });
    }
}
