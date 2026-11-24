package jason.mind.api.infrastructure.adapters.in.web.message;

import jason.mind.api.core.ports.in.message.ListMessagesIn;
import jason.mind.api.core.ports.in.message.ListMessagesPort;
import jason.mind.api.infrastructure.adapters.in.web.WebExchange;
import lombok.RequiredArgsConstructor;
import net.peelweb.context.controller.Controller;
import net.peelweb.context.controller.Mapping;
import net.peelweb.context.endpoint.Request;
import net.peelweb.context.endpoint.Response;
import net.peelweb.context.endpoint.Responses;

/**
 * Controller that exposes the messages exchanged between the agents.
 * Filters the messages by agent, sender and receiver and paginates them.
 *
 * @author Gabriel Lima
 */
@Controller("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    /** Input port that lists messages. */
    private final ListMessagesPort listMessages;

    /**
     * Lists the messages matching the {@code agents}, {@code senders}, {@code receivers}, {@code until},
     * {@code after}, {@code before} and {@code limit} parameters.
     *
     * @param request HTTP request carrying the filter and pagination parameters
     * @return response with the matching messages
     */
    @Mapping
    public Response messages(Request request) {
        return WebExchange.handle("messages", () -> Responses.ok(this.listMessages.execute(
                new ListMessagesIn(WebExchange.set(request, "agents"), WebExchange.set(request, "senders"),
                        WebExchange.set(request, "receivers"), WebExchange.longValue(request, "until"),
                        request.getParameter("after"), request.getParameter("before"),
                        WebExchange.integer(request, "limit")))));
    }
}
