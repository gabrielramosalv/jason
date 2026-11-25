package jason.mind.api.infrastructure.adapters.in.web.cycle;

import jason.mind.api.core.ports.in.cycle.CyclesOut;
import jason.mind.api.core.ports.in.cycle.FindCycleReasoningIn;
import jason.mind.api.core.ports.in.cycle.FindCycleReasoningPort;
import jason.mind.api.core.ports.in.cycle.ListCyclesIn;
import jason.mind.api.core.ports.in.cycle.ListCyclesPort;
import jason.mind.api.infrastructure.adapters.in.web.WebExchange;
import lombok.RequiredArgsConstructor;
import net.peelweb.context.controller.Controller;
import net.peelweb.context.controller.Mapping;
import net.peelweb.context.endpoint.Request;
import net.peelweb.context.endpoint.Response;
import net.peelweb.context.endpoint.Responses;

/**
 * Controller that exposes the reasoning cycles of an agent.
 * Lists the cycles in a range and details the reasoning of a single cycle.
 *
 * @author Gabriel Lima
 */
@Controller("/api/agents")
@RequiredArgsConstructor
public class CycleController {

    /** Input port that lists the cycles of an agent. */
    private final ListCyclesPort listCycles;

    /** Input port that finds the reasoning of a cycle. */
    private final FindCycleReasoningPort findCycleReasoning;

    /**
     * Lists the cycles of the agent named in the path between the {@code from} and {@code to} parameters.
     *
     * @param request HTTP request carrying the agent name and the cycle range
     * @return response with the cycles, or not modified when the ETag of the range still matches
     */
    @Mapping("/{name}/cycles")
    public Response cycles(Request request) {
        return WebExchange.handle("cycles", () -> {
            CyclesOut out = this.listCycles.execute(
                    new ListCyclesIn(request.getPathVariable("name"), WebExchange.integer(request, "from"),
                            WebExchange.integer(request, "to")));
            return WebExchange.withETag(request, out.from() + "-" + out.to() + ":" + out.oldestCycle() + "." + out.newestCycle() + ":"
                    + Integer.toHexString(out.cycles().hashCode()), () -> out);
        });
    }

    /**
     * Returns the reasoning of the agent named in the path at the cycle given in the path.
     *
     * @param request HTTP request carrying the agent name and the cycle number
     * @return response with the reasoning of the cycle
     */
    @Mapping("/{name}/cycles/{cycle}/reasoning")
    public Response getReasoning(Request request) {
        return WebExchange.handle("reasoning", () -> Responses.ok(this.findCycleReasoning.execute(
                new FindCycleReasoningIn(request.getPathVariable("name"), WebExchange.pathInteger(request, "cycle")))));
    }
}
