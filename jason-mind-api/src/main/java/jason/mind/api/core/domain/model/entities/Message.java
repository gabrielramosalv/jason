package jason.mind.api.core.domain.model.entities;

import java.util.Map;
import java.util.Objects;

import jason.mind.api.core.domain.model.timeline.AgentCycle;
import jason.mind.api.core.domain.model.timeline.Entity;
import jason.mind.api.core.domain.model.timeline.Occurrence;
import jason.mind.api.core.domain.model.timeline.Timeline;

public final class Message extends Entity<Message> {

    private final Integer sendNumber;
    private final SendMode sendMode;
    private final Message inReplyTo;
    private final String senderName;
    private final String receiver;
    private final Performative performative;
    private final String content;
    private volatile Occurrence arrival;

    public Message(long id, Occurrence start, Integer sendNumber, SendMode sendMode, Message inReplyTo,
                   String senderName, String receiver, Performative performative, String content) {
        super(id, start);
        if (start == null && senderName == null)
            throw new IllegalArgumentException("Message without start requires senderName [D-098]");
        this.sendNumber = sendNumber;
        this.sendMode = sendMode;
        this.inReplyTo = inReplyTo;
        this.senderName = start == null ? senderName : null;
        this.receiver = Objects.requireNonNull(receiver, "receiver");
        this.performative = Objects.requireNonNull(performative, "performative");
        this.content = content;
    }

    public Integer getSendNumber() { return sendNumber; }

    public SendMode getSendMode() { return sendMode; }

    public Message getInReplyTo() { return inReplyTo; }

    public String getSenderName() { return senderName; }

    public String getSender() {
        return getStart() == null ? senderName : getStart().agent();
    }

    public String getReceiver() { return receiver; }

    public Performative getPerformative() { return performative; }

    public String getContent() { return content; }

    public Occurrence getArrival() { return arrival; }

    public void arrive(Occurrence occurrence) {
        Objects.requireNonNull(occurrence, "arrival");
        if (arrival != null) throw new IllegalStateException("arrival already recorded");
        if (!occurrence.agent().equals(receiver))
            throw new IllegalArgumentException("arrival outside the receiver [INV-030]");
        this.arrival = occurrence;
    }

    @Override
    public String getAgent() {
        return getSender();
    }

    @Override
    protected Occurrence lifeStart() {
        return arrival;
    }

    @Override
    public boolean existsAt(AgentCycle agentCycle) {
        Occurrence s = getStart();
        if (s != null && s.agent().equals(agentCycle.agent()))
            return s.cycle() <= agentCycle.cycle();
        if (receiver.equals(agentCycle.agent())) {
            Occurrence a = arrival;
            if (a == null || a.cycle() > agentCycle.cycle()) return false;
            Occurrence e = getEnd();
            return e == null || e.cycle() >= agentCycle.cycle();
        }
        return false;
    }

    @Override public Map<String, Timeline<?>> timelines() { return Map.of(); }

    @Override public String typeName() { return "message"; }

    @Override public String label() { return performative.name() + "(" + content + ")"; }
}
