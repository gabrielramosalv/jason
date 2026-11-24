package jason.mind.api.infrastructure.adapters.in.web.console;

import jason.mind.api.core.ports.in.console.ListConsoleLinesIn;
import jason.mind.api.core.ports.in.console.ListConsoleLinesPort;
import jason.mind.api.infrastructure.adapters.in.web.WebExchange;
import lombok.RequiredArgsConstructor;
import net.peelweb.context.controller.Controller;
import net.peelweb.context.controller.Mapping;
import net.peelweb.context.endpoint.Request;
import net.peelweb.context.endpoint.Response;
import net.peelweb.context.endpoint.Responses;

/**
 * Controller that exposes the console lines printed by the agents.
 * Filters the lines by agent and sequence range and paginates them.
 *
 * @author Gabriel Lima
 */
@Controller("/api/console")
@RequiredArgsConstructor
public class ConsoleController {

    /** Input port that lists console lines. */
    private final ListConsoleLinesPort listConsoleLines;

    /**
     * Lists the console lines matching the {@code agents}, {@code until}, {@code after}, {@code before}
     * and {@code limit} parameters.
     *
     * @param request HTTP request carrying the filter and pagination parameters
     * @return response with the matching console lines
     */
    @Mapping
    public Response getConsole(Request request) {
        return WebExchange.handle("console", () -> Responses.ok(this.listConsoleLines.execute(
                new ListConsoleLinesIn(WebExchange.set(request, "agents"), WebExchange.longValue(request, "until"),
                        WebExchange.longValue(request, "after"), WebExchange.longValue(request, "before"),
                        WebExchange.integer(request, "limit")))));
    }
}
