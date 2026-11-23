package jason.mind.api.infrastructure.adapters.in.web.message;

import jason.mind.api.core.ports.in.message.ListMessagesIn;
import jason.mind.api.core.ports.in.message.ListMessagesPort;
import jason.mind.api.infrastructure.adapters.in.web.WebExchange;
import net.peelweb.context.controller.Controller;
import net.peelweb.context.controller.Mapping;
import net.peelweb.context.endpoint.Request;
import net.peelweb.context.endpoint.Response;
import net.peelweb.context.endpoint.Responses;

@Controller("/api/messages")
public class MessageController {

    private final ListMessagesPort listMessages;

    public MessageController(ListMessagesPort listMessages) {
        this.listMessages = listMessages;
    }

    @Mapping
    public Response messages(Request request) {
        return WebExchange.handle(() -> Responses.ok(listMessages.execute(new ListMessagesIn(
                WebExchange.set(request, "agents"), WebExchange.set(request, "senders"), WebExchange.set(request, "receivers"),
                WebExchange.longValue(request, "until"), request.getParameter("after"), request.getParameter("before"),
                WebExchange.integer(request, "limit")))));
    }
}
