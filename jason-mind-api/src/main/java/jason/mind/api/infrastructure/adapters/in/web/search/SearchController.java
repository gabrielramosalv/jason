package jason.mind.api.infrastructure.adapters.in.web.search;

import jason.mind.api.core.ports.in.search.SearchDeedExecutionsIn;
import jason.mind.api.core.ports.in.search.SearchDeedExecutionsPort;
import jason.mind.api.core.ports.in.search.SearchEntitiesIn;
import jason.mind.api.core.ports.in.search.SearchEntitiesPort;
import jason.mind.api.infrastructure.adapters.in.web.WebExchange;
import net.peelweb.context.controller.Controller;
import net.peelweb.context.controller.Mapping;
import net.peelweb.context.endpoint.Request;
import net.peelweb.context.endpoint.Response;
import net.peelweb.context.endpoint.Responses;

@Controller("/api/agents")
public class SearchController {

    private final SearchEntitiesPort searchEntities;
    private final SearchDeedExecutionsPort searchDeedExecutions;

    public SearchController(SearchEntitiesPort searchEntities, SearchDeedExecutionsPort searchDeedExecutions) {
        this.searchEntities = searchEntities;
        this.searchDeedExecutions = searchDeedExecutions;
    }

    @Mapping("/{name}/search")
    public Response search(Request request) {
        return WebExchange.handle(() -> Responses.ok(searchEntities.execute(new SearchEntitiesIn(
                request.getPathVariable("name"), request.getParameter("q"), WebExchange.set(request, "types"),
                WebExchange.integer(request, "from"), WebExchange.integer(request, "to"),
                WebExchange.integer(request, "limit")))));
    }

    @Mapping("/{name}/search/deed")
    public Response deed(Request request) {
        return WebExchange.handle(() -> Responses.ok(searchDeedExecutions.execute(new SearchDeedExecutionsIn(
                request.getPathVariable("name"), request.getParameter("plan"), WebExchange.integer(request, "deedIndex")))));
    }
}
