package jason.mind.api.core.ports.in.message;

import java.util.List;

/**
 * Output with a page of messages.
 *
 * @param messages       messages of the page
 * @param counts         sent and received counts per agent
 * @param previousCursor cursor of the previous page, or null
 * @param nextCursor     cursor of the next page, or null
 * @author Gabriel Lima
 */
public record MessagesOut(List<MessageOut> messages, List<CountOut> counts, String previousCursor, String nextCursor) {

    /**
     * Message.
     *
     * @param id           identifier of the message
     * @param sender       name of the sender
     * @param receiver     name of the receiver
     * @param performative performative
     * @param content      content
     * @param status       delivery status
     * @param inReplyTo    identifier of the message being replied, or null
     * @param sent         point of the send, or null
     * @param arrival      point of the arrival, or null
     * @param read         point of the read, or null
     */
    public record MessageOut(String id, String sender, String receiver, String performative, String content,
                             String status, String inReplyTo, PointOut sent, PointOut arrival, PointOut read) {

    }

    /**
     * Point in time of a message event.
     *
     * @param cycle            number of the cycle
     * @param timeMilliseconds wall-clock time, in milliseconds
     */
    public record PointOut(int cycle, long timeMilliseconds) {

    }

    /**
     * Message counts of an agent.
     *
     * @param agent    name of the agent
     * @param sent     number of messages sent
     * @param received number of messages received
     */
    public record CountOut(String agent, int sent, int received) {

    }
}
