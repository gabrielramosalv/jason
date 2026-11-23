package jason.mind.api.infrastructure.adapters.in.web.cycle;

import jason.mind.api.core.ports.in.cycle.CyclesOut;
import jason.mind.api.core.ports.in.cycle.FindCycleReasoningIn;
import jason.mind.api.core.ports.in.cycle.FindCycleReasoningPort;
import jason.mind.api.core.ports.in.cycle.ListCyclesIn;
import jason.mind.api.core.ports.in.cycle.ListCyclesPort;
import jason.mind.api.infrastructure.adapters.in.web.WebExchange;
import net.peelweb.context.controller.Controller;
import net.peelweb.context.controller.Mapping;
import net.peelweb.context.endpoint.Request;
import net.peelweb.context.endpoint.Response;
import net.peelweb.context.endpoint.Responses;

@Controller("/api/agents")
public class CycleController {

    private final ListCyclesPort listCycles;
    private final FindCycleReasoningPort findCycleReasoning;

    public CycleController(ListCyclesPort listCycles, FindCycleReasoningPort findCycleReasoning) {
        this.listCycles = listCycles;
        this.findCycleReasoning = findCycleReasoning;
    }

    @Mapping("/{name}/cycles")
    public Response cycles(Request request) {
        return WebExchange.handle(() -> {
            CyclesOut out = listCycles.execute(new ListCyclesIn(request.getPathVariable("name"),
                    WebExchange.integer(request, "from"), WebExchange.integer(request, "to")));
            return WebExchange.withETag(request, out.from() + "-" + out.to(), () -> out);
        });
    }

    @Mapping("/{name}/cycles/{cycle}/reasoning")
    public Response reasoning(Request request) {
        return WebExchange.handle(() -> Responses.ok(findCycleReasoning.execute(
                new FindCycleReasoningIn(request.getPathVariable("name"), WebExchange.pathInteger(request, "cycle")))));
    }
}
