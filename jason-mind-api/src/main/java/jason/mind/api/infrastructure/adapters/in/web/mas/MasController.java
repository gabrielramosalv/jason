package jason.mind.api.infrastructure.adapters.in.web.mas;

import jason.mind.api.core.ports.in.mas.FindMasPort;
import jason.mind.api.core.ports.in.mas.MasOut;
import jason.mind.api.infrastructure.adapters.in.web.WebExchange;
import net.peelweb.context.controller.Controller;
import net.peelweb.context.controller.Mapping;
import net.peelweb.context.endpoint.Request;
import net.peelweb.context.endpoint.Response;

@Controller("/api/mas")
public class MasController {

    private final FindMasPort findMas;

    public MasController(FindMasPort findMas) {
        this.findMas = findMas;
    }

    @Mapping
    public Response mas(Request request) {
        return WebExchange.handle(() -> {
            MasOut out = findMas.execute();
            StringBuilder tag = new StringBuilder("mas");
            for (MasOut.AgentItem a : out.agents())
                tag.append(':').append(a.name()).append('.').append(a.oldestCycle()).append('.').append(a.newestCycle())
                        .append(a.ended() ? ".e" : "").append(a.active() ? "" : ".i");
            return WebExchange.withETag(request, Integer.toHexString(tag.toString().hashCode()), () -> out);
        });
    }
}
