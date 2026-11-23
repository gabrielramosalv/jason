package jason.mind.api.core.ports.in.message;

import java.util.List;

public record MessagesOut(List<MessageOut> messages, List<CountOut> counts, String previousCursor, String nextCursor) {

    public record MessageOut(String id, String sender, String receiver, String performative, String content, String status,
                             String inReplyTo, PointOut sent, PointOut arrival, PointOut read) {
    }

    public record PointOut(int cycle, long timeMilliseconds) {
    }

    public record CountOut(String agent, int sent, int received) {
    }
}
