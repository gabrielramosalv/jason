package jason.mind.api.infrastructure.adapters.in.web.source;

import jason.mind.api.core.ports.in.source.FindSourcesIn;
import jason.mind.api.core.ports.in.source.FindSourcesPort;
import jason.mind.api.core.ports.in.source.SourcesOut;
import jason.mind.api.infrastructure.adapters.in.web.WebExchange;
import net.peelweb.context.controller.Controller;
import net.peelweb.context.controller.Mapping;
import net.peelweb.context.endpoint.Request;
import net.peelweb.context.endpoint.Response;

@Controller("/api/agents")
public class SourceController {

    private final FindSourcesPort findSources;

    public SourceController(FindSourcesPort findSources) {
        this.findSources = findSources;
    }

    @Mapping("/{name}/sources")
    public Response sources(Request request) {
        return WebExchange.handle(() -> {
            SourcesOut out = findSources.execute(new FindSourcesIn(request.getPathVariable("name")));
            int h = 1;
            for (SourcesOut.FileOut f : out.files()) h = 31 * h + f.path().hashCode() * 17 + f.content().length();
            return WebExchange.withETag(request, "sources:" + Integer.toHexString(h), () -> out);
        });
    }
}
