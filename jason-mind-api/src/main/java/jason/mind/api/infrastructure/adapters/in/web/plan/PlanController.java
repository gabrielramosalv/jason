package jason.mind.api.infrastructure.adapters.in.web.plan;

import jason.mind.api.core.ports.in.plan.FindPlansIn;
import jason.mind.api.core.ports.in.plan.FindPlansPort;
import jason.mind.api.core.ports.in.plan.PlansOut;
import jason.mind.api.infrastructure.adapters.in.web.WebExchange;
import lombok.RequiredArgsConstructor;
import net.peelweb.context.controller.Controller;
import net.peelweb.context.controller.Mapping;
import net.peelweb.context.endpoint.Request;
import net.peelweb.context.endpoint.Response;

/**
 * Controller that exposes the plan library of an agent.
 * Answers with an ETag derived from the version of the plan library.
 *
 * @author Gabriel Lima
 */
@Controller("/api/agents")
@RequiredArgsConstructor
public class PlanController {

    /** Input port that finds the plans of an agent. */
    private final FindPlansPort findPlans;

    /**
     * Returns the plans of the agent named in the path.
     *
     * @param request HTTP request carrying the agent name
     * @return response with the plans, or not modified when the ETag of the version still matches
     */
    @Mapping("/{name}/plans")
    public Response listPlans(Request request) {
        return WebExchange.handle("plans", () -> {
            PlansOut out = this.findPlans.execute(new FindPlansIn(request.getPathVariable("name")));
            return WebExchange.withETag(request, "plans:" + out.version(), () -> out);
        });
    }
}
