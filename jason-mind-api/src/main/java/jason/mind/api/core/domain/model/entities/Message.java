package jason.mind.api.core.domain.model.entities;

import jason.mind.api.core.domain.model.timeline.AgentCycle;
import jason.mind.api.core.domain.model.timeline.Entity;
import jason.mind.api.core.domain.model.timeline.Occurrence;
import jason.mind.api.core.domain.model.timeline.Timeline;
import lombok.Getter;

import java.util.Map;
import java.util.Objects;

/**
 * Message exchanged between agents.
 * Its start belongs to the sender and its arrival to the receiver; a message without start keeps the sender name.
 *
 * @author Gabriel Lima
 */
@Getter
public final class Message extends Entity {

    /** Number of the send operation that produced the message. */
    private final Integer sendNumber;

    /** Mode of the send operation. */
    private final SendMode sendMode;

    /** Message this one replies to, or null. */
    private final Message inReplyTo;

    /** Name of the sender, kept only when the message has no start occurrence. */
    private final String senderName;

    /** Name of the receiving agent. */
    private final String receiver;

    /** Performative of the message. */
    private final Performative performative;

    /** Content of the message. */
    private final String content;

    /** Occurrence of the arrival at the receiver, or null while not arrived. */
    private volatile Occurrence arrival;

    /**
     * Creates the message with its identifier, start occurrence and send data.
     *
     * @param id           unique identifier of the entity
     * @param start        occurrence of the send at the sender, or null when the sender is not recorded
     * @param sendNumber   {@link #sendNumber}
     * @param sendMode     {@link #sendMode}
     * @param inReplyTo    {@link #inReplyTo}
     * @param senderName   name of the sender, required when the start is null and ignored otherwise
     * @param receiver     {@link #receiver}
     * @param performative {@link #performative}
     * @param content      {@link #content}
     * @throws IllegalArgumentException if both the start and the sender name are null
     * @throws NullPointerException     if the receiver or the performative is null
     */
    public Message(long id, Occurrence start, Integer sendNumber, SendMode sendMode, Message inReplyTo,
                   String senderName, String receiver, Performative performative, String content) {
        super(id, start);
        if (start == null && senderName == null) {
            throw new IllegalArgumentException("Message without start requires senderName");
        }
        this.sendNumber = sendNumber;
        this.sendMode = sendMode;
        this.inReplyTo = inReplyTo;
        this.senderName = start == null ? senderName : null;
        this.receiver = Objects.requireNonNull(receiver, "receiver");
        this.performative = Objects.requireNonNull(performative, "performative");
        this.content = content;
    }

    /**
     * Records the arrival of the message at the receiver.
     *
     * @param occurrence occurrence of the arrival
     * @throws NullPointerException     if the occurrence is null
     * @throws IllegalStateException    if the arrival was already recorded
     * @throws IllegalArgumentException if the occurrence does not belong to the receiver
     */
    public void arrive(Occurrence occurrence) {
        Objects.requireNonNull(occurrence, "arrival");
        if (this.arrival != null) {
            throw new IllegalStateException("arrival already recorded");
        }
        if (!occurrence.agent().equals(this.receiver)) {
            throw new IllegalArgumentException("arrival outside the receiver");
        }
        this.arrival = occurrence;
    }

    /**
     * Returns the name of the sender.
     *
     * @return agent of the start occurrence, or the sender name when there is no start
     */
    public String getSender() {
        return this.getStart() == null ? this.senderName : this.getStart().agent();
    }

    @Override
    public boolean existsAt(AgentCycle agentCycle) {
        Occurrence s = this.getStart();
        if (s != null && s.agent().equals(agentCycle.agent())) {
            return s.cycle() <= agentCycle.cycle();
        }
        if (this.receiver.equals(agentCycle.agent())) {
            Occurrence a = this.arrival;
            if (a == null || a.cycle() > agentCycle.cycle()) {
                return false;
            }
            Occurrence e = this.getEnd();
            return e == null || e.cycle() >= agentCycle.cycle();
        }
        return false;
    }

    @Override
    protected Occurrence lifeStart() {
        return this.arrival;
    }

    @Override
    public Map<String, Timeline<?>> timelines() {
        return Map.of();
    }

    @Override
    public String typeName() {
        return "message";
    }

    @Override
    public String label() {
        return this.performative.name() + "(" + this.content + ")";
    }

    @Override
    public String getAgent() {
        return this.getSender();
    }
}
