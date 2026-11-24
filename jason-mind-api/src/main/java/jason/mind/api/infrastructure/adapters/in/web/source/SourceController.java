package jason.mind.api.infrastructure.adapters.in.web.source;

import jason.mind.api.core.ports.in.source.FindSourcesIn;
import jason.mind.api.core.ports.in.source.FindSourcesPort;
import jason.mind.api.core.ports.in.source.SourcesOut;
import jason.mind.api.infrastructure.adapters.in.web.WebExchange;
import lombok.RequiredArgsConstructor;
import net.peelweb.context.controller.Controller;
import net.peelweb.context.controller.Mapping;
import net.peelweb.context.endpoint.Request;
import net.peelweb.context.endpoint.Response;

/**
 * Controller that exposes the source files of an agent.
 * Answers with an ETag derived from the paths and sizes of the files.
 *
 * @author Gabriel Lima
 */
@Controller("/api/agents")
@RequiredArgsConstructor
public class SourceController {

    /** Input port that finds the source files of an agent. */
    private final FindSourcesPort findSources;

    /**
     * Returns the source files of the agent named in the path.
     *
     * @param request HTTP request carrying the agent name
     * @return response with the source files, or not modified when the ETag still matches
     */
    @Mapping("/{name}/sources")
    public Response sources(Request request) {
        return WebExchange.handle("sources", () -> {
            SourcesOut out = this.findSources.execute(new FindSourcesIn(request.getPathVariable("name")));
            int h = 1;
            for (SourcesOut.FileOut f : out.files()) {
                h = 31 * h + f.path().hashCode() * 17 + f.content().length();
            }
            return WebExchange.withETag(request, "sources:" + Integer.toHexString(h), () -> out);
        });
    }
}
