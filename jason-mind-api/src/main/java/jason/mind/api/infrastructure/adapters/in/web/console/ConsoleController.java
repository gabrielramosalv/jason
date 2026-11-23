package jason.mind.api.infrastructure.adapters.in.web.console;

import jason.mind.api.core.ports.in.console.ListConsoleLinesIn;
import jason.mind.api.core.ports.in.console.ListConsoleLinesPort;
import jason.mind.api.infrastructure.adapters.in.web.WebExchange;
import net.peelweb.context.controller.Controller;
import net.peelweb.context.controller.Mapping;
import net.peelweb.context.endpoint.Request;
import net.peelweb.context.endpoint.Response;
import net.peelweb.context.endpoint.Responses;

@Controller("/api/console")
public class ConsoleController {

    private final ListConsoleLinesPort listConsoleLines;

    public ConsoleController(ListConsoleLinesPort listConsoleLines) {
        this.listConsoleLines = listConsoleLines;
    }

    @Mapping
    public Response console(Request request) {
        return WebExchange.handle(() -> Responses.ok(listConsoleLines.execute(new ListConsoleLinesIn(
                WebExchange.set(request, "agents"), WebExchange.longValue(request, "until"),
                WebExchange.longValue(request, "after"), WebExchange.longValue(request, "before"),
                WebExchange.integer(request, "limit")))));
    }
}
