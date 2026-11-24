package jason.mind.api.infrastructure.adapters.in.web.search;

import jason.mind.api.core.ports.in.search.SearchDeedExecutionsIn;
import jason.mind.api.core.ports.in.search.SearchDeedExecutionsPort;
import jason.mind.api.core.ports.in.search.SearchEntitiesIn;
import jason.mind.api.core.ports.in.search.SearchEntitiesPort;
import jason.mind.api.infrastructure.adapters.in.web.WebExchange;
import lombok.RequiredArgsConstructor;
import net.peelweb.context.controller.Controller;
import net.peelweb.context.controller.Mapping;
import net.peelweb.context.endpoint.Request;
import net.peelweb.context.endpoint.Response;
import net.peelweb.context.endpoint.Responses;

/**
 * Controller that exposes the searches over the history of an agent.
 * Searches entities by text and type, and the executions of a deed of a plan.
 *
 * @author Gabriel Lima
 */
@Controller("/api/agents")
@RequiredArgsConstructor
public class SearchController {

    /** Input port that searches entities. */
    private final SearchEntitiesPort searchEntities;

    /** Input port that searches the executions of a deed. */
    private final SearchDeedExecutionsPort searchDeedExecutions;

    /**
     * Searches the entities of the agent named in the path matching the {@code q}, {@code types},
     * {@code from}, {@code to} and {@code limit} parameters.
     *
     * @param request HTTP request carrying the agent name and the search parameters
     * @return response with the matching entities
     */
    @Mapping("/{name}/search")
    public Response search(Request request) {
        return WebExchange.handle("search", () -> Responses.ok(this.searchEntities.execute(
                new SearchEntitiesIn(request.getPathVariable("name"), request.getParameter("q"),
                        WebExchange.set(request, "types"), WebExchange.integer(request, "from"),
                        WebExchange.integer(request, "to"), WebExchange.integer(request, "limit")))));
    }

    /**
     * Searches the executions of the deed at the {@code deedIndex} parameter of the plan given by the
     * {@code plan} parameter, for the agent named in the path.
     *
     * @param request HTTP request carrying the agent name, the plan and the deed index
     * @return response with the executions of the deed
     */
    @Mapping("/{name}/search/deed")
    public Response searchDeeds(Request request) {
        return WebExchange.handle("searchDeed", () -> Responses.ok(this.searchDeedExecutions.execute(
                new SearchDeedExecutionsIn(request.getPathVariable("name"), request.getParameter("plan"),
                        WebExchange.integer(request, "deedIndex")))));
    }
}
