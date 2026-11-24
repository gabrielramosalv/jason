package jason.mind.api.core.application.capture;

import jason.mind.api.core.domain.model.entities.Action;
import jason.mind.api.core.domain.model.entities.Belief;
import jason.mind.api.core.domain.model.entities.Binding;
import jason.mind.api.core.domain.model.entities.Event;
import jason.mind.api.core.domain.model.entities.IntendedMeans;
import jason.mind.api.core.domain.model.entities.Intention;
import jason.mind.api.core.domain.model.entities.Message;
import jason.mind.api.core.domain.model.entities.Plan;
import jason.mind.api.core.domain.model.entities.Timer;
import jason.mind.api.core.domain.model.history.Agent;
import jason.mind.api.core.domain.model.history.OccurrenceRecord;
import jason.mind.api.core.domain.model.program.Annotation;
import jason.mind.api.core.domain.model.program.Source;
import jason.mind.api.core.domain.model.timeline.CollectionChange;
import jason.mind.api.core.domain.model.timeline.CollectionTimeline;
import jason.mind.api.core.domain.model.timeline.Entity;
import jason.mind.api.core.domain.model.timeline.Occurrence;
import jason.mind.api.core.domain.model.timeline.Timeline;
import jason.mind.api.core.domain.service.history.HistoryRecorderDomainService;
import jason.mind.api.core.ports.in.capture.EntityRef;
import jason.mind.api.core.ports.in.capture.EntitySpec;
import jason.mind.api.core.ports.in.capture.RecordOccurrenceIn;
import jason.mind.api.core.ports.in.capture.RecordOccurrenceOut;
import jason.mind.api.core.ports.in.capture.RecordOccurrencePort;
import jason.mind.api.core.ports.out.history.HistoryRepositoryPort;
import jason.mind.api.core.ports.out.sequence.EntityIdProviderPort;
import jason.mind.api.core.ports.out.sequence.SequenceProviderPort;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Use case that records an occurrence in the history of an agent.
 * It handles entity births, timeline and collection changes, message arrivals, new bindings and
 * entity ends, stamping each one with the next sequence of the agent. Changes that do not alter
 * the current value are ignored and reported with sequence zero.
 *
 * @author Gabriel Lima
 */
public class RecordOccurrenceUseCase implements RecordOccurrencePort {

    /** History where entities and occurrences are recorded. */
    private final HistoryRepositoryPort historyRepository;

    /** Source of the sequence numbers of each agent. */
    private final SequenceProviderPort sequenceProvider;

    /** Source of the ids of new entities. */
    private final EntityIdProviderPort entityIdProvider;

    /** Domain service that validates each occurrence before it is recorded. */
    private final HistoryRecorderDomainService recorder;

    /** Resolver of the entities and causes referenced by the input. */
    private final CauseResolver causes;

    /** Last sequence recorded for each agent, keyed by agent name. */
    private final Map<String, Long> lastSequence = new ConcurrentHashMap<>();

    /**
     * Creates the use case with the history, the id and sequence providers and the recorder service.
     *
     * @param historyRepository {@link #historyRepository}
     * @param sequenceProvider  {@link #sequenceProvider}
     * @param entityIdProvider  {@link #entityIdProvider}
     * @param recorder          {@link #recorder}
     */
    public RecordOccurrenceUseCase(HistoryRepositoryPort historyRepository, SequenceProviderPort sequenceProvider,
                                   EntityIdProviderPort entityIdProvider, HistoryRecorderDomainService recorder) {
        this.historyRepository = historyRepository;
        this.sequenceProvider = sequenceProvider;
        this.entityIdProvider = entityIdProvider;
        this.recorder = recorder;
        this.causes = new CauseResolver(historyRepository);
    }

    /**
     * Creates the next occurrence of an agent, validates it against the target entity and remembers its sequence.
     *
     * @param agent  agent where the occurrence happens
     * @param target entity affected by the occurrence, or {@code null} for a birth
     * @param in     input carrying the moment of the occurrence
     * @param cause  supplier of the cause of the occurrence
     * @return the created occurrence
     */
    private Occurrence occurrence(Agent agent, Entity target, RecordOccurrenceIn in, Occurrence.CauseSupplier cause) {
        long seq = this.sequenceProvider.next(agent.getName());
        Occurrence occ = new Occurrence(in.moment(), seq, cause.get());
        this.recorder.check(agent, target, occ, this.lastSequence.getOrDefault(agent.getName(), 0L));
        this.lastSequence.put(agent.getName(), seq);
        return occ;
    }

    /**
     * Records the birth of an entity. A message spec with a sender is registered as an incoming
     * message of its receiver without an occurrence of its own.
     *
     * @param agent agent where the entity is born
     * @param in    birth input with the entity spec
     * @return the id of the new entity and the sequence of its birth, or zero for an incoming message
     */
    private RecordOccurrenceOut recordBirth(Agent agent, RecordOccurrenceIn.Birth in) {
        String name = agent.getName();
        long id = this.entityIdProvider.next();
        if (in.spec() instanceof EntitySpec.MessageSpec(
                Integer sendNumber, jason.mind.api.core.domain.model.entities.SendMode sendMode, Long inReplyToId,
                String senderName, String receiver, jason.mind.api.core.domain.model.entities.Performative performative,
                String content
        ) && senderName != null) {
            Message m = new Message(id, null, sendNumber, sendMode, this.inReplyTo(name, inReplyToId), senderName,
                    receiver, performative, content);
            this.historyRepository.registerMessage(receiver, m);
            return new RecordOccurrenceOut(id, 0);
        }
        Occurrence occ = this.occurrence(agent, null, in, () -> this.causes.resolve(name, in.cause()));
        Entity e = switch (in.spec()) {
            case EntitySpec.BeliefSpec s -> {
                Belief b = new Belief(id, occ, s.content(), s.rule(), s.orderKey());
                for (Annotation a : s.annotations()) {
                    if (!b.getAnnotations().current().contains(a)) {
                        b.getAnnotations().add(a, occ);
                    }
                }
                yield b;
            }
            case EntitySpec.EventSpec s -> new Event(id, occ, s.trigger(), s.atomic(), s.place());
            case EntitySpec.IntentionSpec s -> new Intention(id, occ, s.place());
            case EntitySpec.IntendedMeansSpec s -> {
                IntendedMeans im = new IntendedMeans(id, occ, (Plan) this.causes.entity(name, s.planId()));
                ((Intention) this.causes.entity(name, s.intentionId())).push(im);
                yield im;
            }
            case EntitySpec.PlanSpec s -> {
                Plan p = new Plan(id, occ, s.label(), s.orderKey(), s.trigger(), s.context(), s.body(), s.annotations(),
                        s.source());
                for (Source src : s.sources()) {
                    if (!p.getSources().current().contains(src)) {
                        p.getSources().add(src, occ);
                    }
                }
                yield p;
            }
            case EntitySpec.ActionSpec s -> new Action(id, occ, s.action());
            case EntitySpec.TimerSpec s -> new Timer(id, occ, s.trigger(), s.deadlineMilliseconds());
            case EntitySpec.MessageSpec s -> new Message(id, occ, s.sendNumber(), s.sendMode(),
                    this.inReplyTo(name, s.inReplyToId()), null, s.receiver(), s.performative(), s.content());
        };
        this.historyRepository.appendEntity(name, e);
        if (e instanceof Message m) {
            this.historyRepository.registerMessage(m.getReceiver(), m);
        }
        this.historyRepository.appendOccurrence(name,
                new OccurrenceRecord(e, OccurrenceRecord.Kind.START, null, null, occ));
        return new RecordOccurrenceOut(id, occ.sequence());
    }

    /**
     * Finds the message a new message replies to.
     *
     * @param agent name of the agent whose history holds the original message
     * @param id    id of the original message, possibly {@code null}
     * @return the original message, or {@code null} when there is no id or it does not refer to a message
     */
    private Message inReplyTo(String agent, Long id) {
        if (id == null) {
            return null;
        }
        return this.historyRepository.findEntity(agent, id).filter(Message.class::isInstance).map(Message.class::cast)
                .orElse(null);
    }

    /**
     * Records a new value in a timeline of an entity. A value equal to the current one is ignored.
     *
     * @param agent agent that owns the entity
     * @param in    change input with the timeline name and the new value
     * @return the entity id and the sequence of the change, or zero when the value did not change
     * @throws IllegalArgumentException if the entity has no timeline with the given name
     */
    @SuppressWarnings("unchecked")
    private RecordOccurrenceOut recordChange(Agent agent, RecordOccurrenceIn.Change in) {
        String name = agent.getName();
        Entity e = this.causes.entity(name, in.entityId());
        Timeline<Object> tl = (Timeline<Object>) e.timelines().get(in.timeline());
        if (tl == null) {
            throw new IllegalArgumentException(e.typeName() + " has no timeline " + in.timeline());
        }
        Object value = in.value() instanceof EntityRef(long id) ? this.causes.entity(name, id) : in.value();
        if (!tl.isEmpty() && Objects.equals(tl.current(), value)) {
            return new RecordOccurrenceOut(e.getId(), 0);
        }
        Occurrence occ = this.occurrence(agent, e, in, () -> this.causes.resolve(name, in.cause()));
        tl.put(occ, value);
        this.historyRepository.appendOccurrence(name,
                new OccurrenceRecord(e, OccurrenceRecord.Kind.CHANGE, in.timeline(), value, occ));
        return new RecordOccurrenceOut(e.getId(), occ.sequence());
    }

    /**
     * Records the addition or removal of an element in a collection of an entity. An operation that
     * does not change the collection is ignored.
     *
     * @param agent agent that owns the entity
     * @param in    collection change input with the collection name, the element and the operation
     * @return the entity id and the sequence of the change, or zero when the collection did not change
     * @throws IllegalArgumentException if the entity has no collection with the given name
     */
    @SuppressWarnings("unchecked")
    private RecordOccurrenceOut recordCollectionChange(Agent agent, RecordOccurrenceIn.CollectionChange in) {
        String name = agent.getName();
        Entity e = this.causes.entity(name, in.entityId());
        CollectionTimeline<Object> ct = (CollectionTimeline<Object>) e.collections().get(in.collection());
        if (ct == null) {
            throw new IllegalArgumentException(e.typeName() + " has no collection " + in.collection());
        }
        boolean present = ct.current().contains(in.element());
        if (present == in.added()) {
            return new RecordOccurrenceOut(e.getId(), 0);
        }
        Occurrence occ = this.occurrence(agent, e, in, () -> this.causes.resolve(name, in.cause()));
        if (in.added()) {
            ct.add(in.element(), occ);
        } else {
            ct.remove(in.element(), occ);
        }
        CollectionChange<Object> change = new CollectionChange<>(in.element(),
                in.added() ? CollectionChange.Operation.ADDED : CollectionChange.Operation.REMOVED);
        this.historyRepository.appendOccurrence(name,
                new OccurrenceRecord(e, OccurrenceRecord.Kind.CHANGE, in.collection(), change, occ));
        return new RecordOccurrenceOut(e.getId(), occ.sequence());
    }

    /**
     * Records the arrival of a message at the agent.
     *
     * @param agent agent that receives the message
     * @param in    arrival input with the message id
     * @return the message id and the sequence of the arrival
     */
    private RecordOccurrenceOut recordArrival(Agent agent, RecordOccurrenceIn.Arrival in) {
        String name = agent.getName();
        Message m = (Message) this.causes.entity(name, in.messageId());
        Occurrence occ = this.occurrence(agent, m, in, () -> null);
        m.arrive(occ);
        this.historyRepository.appendOccurrence(name,
                new OccurrenceRecord(m, OccurrenceRecord.Kind.ARRIVAL, null, null, occ));
        return new RecordOccurrenceOut(m.getId(), occ.sequence());
    }

    /**
     * Records a new variable binding in an intended means.
     *
     * @param agent agent that owns the intended means
     * @param in    binding input with the variable name and value
     * @return the intended means id and the sequence of the binding
     */
    private RecordOccurrenceOut addBinding(Agent agent, RecordOccurrenceIn.AddBinding in) {
        IntendedMeans im = (IntendedMeans) this.causes.entity(agent.getName(), in.intendedMeansId());
        long seq = this.sequenceProvider.next(agent.getName());
        this.lastSequence.put(agent.getName(), seq);
        im.addBinding(new Binding(in.name(), in.value(), in.moment(), seq));
        return new RecordOccurrenceOut(im.getId(), seq);
    }

    /**
     * Records the end of an entity.
     *
     * @param agent agent that owns the entity
     * @param in    end input with the entity id and cause
     * @return the entity id and the sequence of its end
     */
    private RecordOccurrenceOut recordEnd(Agent agent, RecordOccurrenceIn.End in) {
        String name = agent.getName();
        Entity e = this.causes.entity(name, in.entityId());
        Occurrence occ = this.occurrence(agent, e, in, () -> this.causes.resolve(name, in.cause()));
        e.finish(occ);
        this.historyRepository.appendOccurrence(name,
                new OccurrenceRecord(e, OccurrenceRecord.Kind.END, null, null, occ));
        return new RecordOccurrenceOut(e.getId(), occ.sequence());
    }

    @Override
    public RecordOccurrenceOut execute(RecordOccurrenceIn in) {
        String agentName = in.agent();
        Agent agent = this.historyRepository.loadAgent(agentName).orElseThrow(
                () -> new UnknownAgentException(agentName));
        return switch (in) {
            case RecordOccurrenceIn.Birth b -> this.recordBirth(agent, b);
            case RecordOccurrenceIn.Change c -> this.recordChange(agent, c);
            case RecordOccurrenceIn.CollectionChange c -> this.recordCollectionChange(agent, c);
            case RecordOccurrenceIn.Arrival a -> this.recordArrival(agent, a);
            case RecordOccurrenceIn.AddBinding b -> this.addBinding(agent, b);
            case RecordOccurrenceIn.End e -> this.recordEnd(agent, e);
        };
    }
}
