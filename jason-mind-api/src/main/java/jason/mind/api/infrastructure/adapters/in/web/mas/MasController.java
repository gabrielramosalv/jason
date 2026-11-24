package jason.mind.api.infrastructure.adapters.in.web.mas;

import jason.mind.api.core.ports.in.mas.FindMasPort;
import jason.mind.api.core.ports.in.mas.MasOut;
import jason.mind.api.infrastructure.adapters.in.web.WebExchange;
import lombok.RequiredArgsConstructor;
import net.peelweb.context.controller.Controller;
import net.peelweb.context.controller.Mapping;
import net.peelweb.context.endpoint.Request;
import net.peelweb.context.endpoint.Response;

/**
 * Controller that exposes the overview of the multi-agent system.
 * Answers with an ETag derived from the name, cycle range and status of every agent.
 *
 * @author Gabriel Lima
 */
@Controller("/api/mas")
@RequiredArgsConstructor
public class MasController {

    /** Input port that finds the overview of the multi-agent system. */
    private final FindMasPort findMas;

    /**
     * Returns the overview of the multi-agent system.
     *
     * @param request HTTP request, read for its conditional headers
     * @return response with the overview, or not modified when the ETag still matches
     */
    @Mapping
    public Response getMas(Request request) {
        return WebExchange.handle("mas", () -> {
            MasOut out = this.findMas.execute();
            StringBuilder tag = new StringBuilder("mas");
            for (MasOut.AgentItem a : out.agents()) {
                tag.append(':').append(a.name()).append('.').append(a.oldestCycle()).append('.').append(a.newestCycle())
                        .append(a.ended() ? ".e" : "").append(a.active() ? "" : ".i");
            }
            return WebExchange.withETag(request, Integer.toHexString(tag.toString().hashCode()), () -> out);
        });
    }
}
