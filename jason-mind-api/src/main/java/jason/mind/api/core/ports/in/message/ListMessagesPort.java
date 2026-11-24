package jason.mind.api.core.ports.in.message;

/**
 * Port for listing messages exchanged between agents.
 *
 * @author Gabriel Lima
 */
public interface ListMessagesPort {

    /**
     * Lists a page of messages.
     *
     * @param in filters and pagination
     * @return page of messages with counts and cursors
     */
    MessagesOut execute(ListMessagesIn in);
}
