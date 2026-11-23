package jason.mind.api.core.ports.in.message;

public interface ListMessagesPort {

    MessagesOut execute(ListMessagesIn in);
}
