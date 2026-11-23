package jason.mind.api.core.application.capture;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

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

public class RecordOccurrenceUseCase implements RecordOccurrencePort {

    private final HistoryRepositoryPort historyRepository;
    private final SequenceProviderPort sequenceProvider;
    private final EntityIdProviderPort entityIdProvider;
    private final HistoryRecorderDomainService recorder;
    private final CauseResolver causes;
    private final Map<String, Long> lastSequence = new ConcurrentHashMap<>();

    public RecordOccurrenceUseCase(HistoryRepositoryPort historyRepository, SequenceProviderPort sequenceProvider,
                                   EntityIdProviderPort entityIdProvider, HistoryRecorderDomainService recorder) {
        this.historyRepository = historyRepository;
        this.sequenceProvider = sequenceProvider;
        this.entityIdProvider = entityIdProvider;
        this.recorder = recorder;
        this.causes = new CauseResolver(historyRepository);
    }

    @Override
    public RecordOccurrenceOut execute(RecordOccurrenceIn in) {
        String agentName = in.agent();
        Agent agent = historyRepository.loadAgent(agentName).orElseThrow(() -> new UnknownAgentException(agentName));
        return switch (in) {
            case RecordOccurrenceIn.Birth b -> birth(agent, b);
            case RecordOccurrenceIn.Change c -> change(agent, c);
            case RecordOccurrenceIn.CollectionChange c -> collectionChange(agent, c);
            case RecordOccurrenceIn.Arrival a -> arrival(agent, a);
            case RecordOccurrenceIn.AddBinding b -> addBinding(agent, b);
            case RecordOccurrenceIn.End e -> end(agent, e);
        };
    }

    private Occurrence occurrence(Agent agent, Entity<?> target, RecordOccurrenceIn in, Occurrence.CauseSupplier cause) {
        long seq = sequenceProvider.next(agent.getName());
        Occurrence occ = new Occurrence(in.moment(), seq, cause.get());
        recorder.check(agent, target, occ, lastSequence.getOrDefault(agent.getName(), 0L));
        lastSequence.put(agent.getName(), seq);
        return occ;
    }

    private RecordOccurrenceOut birth(Agent agent, RecordOccurrenceIn.Birth in) {
        String name = agent.getName();
        long id = entityIdProvider.next();
        if (in.spec() instanceof EntitySpec.MessageSpec ms && ms.senderName() != null) {
            Message m = new Message(id, null, ms.sendNumber(), ms.sendMode(), inReplyTo(name, ms.inReplyToId()), ms.senderName(),
                    ms.receiver(), ms.performative(), ms.content());
            historyRepository.registerMessage(ms.receiver(), m);
            return new RecordOccurrenceOut(id, 0);
        }
        Occurrence occ = occurrence(agent, null, in, () -> causes.resolve(name, in.cause()));
        Entity<?> e = switch (in.spec()) {
            case EntitySpec.BeliefSpec s -> {
                Belief b = new Belief(id, occ, s.content(), s.rule(), s.orderKey());
                for (Annotation a : s.annotations())
                    if (!b.getAnnotations().current().contains(a)) b.getAnnotations().add(a, occ);
                yield b;
            }
            case EntitySpec.EventSpec s -> new Event(id, occ, s.trigger(), s.atomic(), s.place());
            case EntitySpec.IntentionSpec s -> new Intention(id, occ, s.place());
            case EntitySpec.IntendedMeansSpec s -> {
                IntendedMeans im = new IntendedMeans(id, occ, (Plan) causes.entity(name, s.planId()));
                ((Intention) causes.entity(name, s.intentionId())).push(im);
                yield im;
            }
            case EntitySpec.PlanSpec s -> {
                Plan p = new Plan(id, occ, s.label(), s.orderKey(), s.trigger(), s.context(), s.body(), s.annotations(), s.source());
                for (Source src : s.sources())
                    if (!p.getSources().current().contains(src)) p.getSources().add(src, occ);
                yield p;
            }
            case EntitySpec.ActionSpec s -> new Action(id, occ, s.action());
            case EntitySpec.TimerSpec s -> new Timer(id, occ, s.trigger(), s.deadlineMilliseconds());
            case EntitySpec.MessageSpec s -> new Message(id, occ, s.sendNumber(), s.sendMode(), inReplyTo(name, s.inReplyToId()),
                    null, s.receiver(), s.performative(), s.content());
        };
        historyRepository.appendEntity(name, e);
        if (e instanceof Message m) historyRepository.registerMessage(m.getReceiver(), m);
        historyRepository.appendOccurrence(name, new OccurrenceRecord(e, OccurrenceRecord.Kind.START, null, null, occ));
        return new RecordOccurrenceOut(id, occ.sequence());
    }

    private Message inReplyTo(String agent, Long id) {
        if (id == null) return null;
        return historyRepository.findEntity(agent, id).filter(Message.class::isInstance).map(Message.class::cast).orElse(null);
    }

    @SuppressWarnings("unchecked")
    private RecordOccurrenceOut change(Agent agent, RecordOccurrenceIn.Change in) {
        String name = agent.getName();
        Entity<?> e = causes.entity(name, in.entityId());
        Timeline<Object> tl = (Timeline<Object>) e.timelines().get(in.timeline());
        if (tl == null) throw new IllegalArgumentException(e.typeName() + " has no timeline " + in.timeline());
        Object value = in.value() instanceof EntityRef ref ? causes.entity(name, ref.id()) : in.value();
        if (!tl.isEmpty() && Objects.equals(tl.current(), value)) return new RecordOccurrenceOut(e.getId(), 0);
        Occurrence occ = occurrence(agent, e, in, () -> causes.resolve(name, in.cause()));
        tl.put(occ, value);
        historyRepository.appendOccurrence(name, new OccurrenceRecord(e, OccurrenceRecord.Kind.CHANGE, in.timeline(), value, occ));
        return new RecordOccurrenceOut(e.getId(), occ.sequence());
    }

    @SuppressWarnings("unchecked")
    private RecordOccurrenceOut collectionChange(Agent agent, RecordOccurrenceIn.CollectionChange in) {
        String name = agent.getName();
        Entity<?> e = causes.entity(name, in.entityId());
        CollectionTimeline<Object> ct = (CollectionTimeline<Object>) e.collections().get(in.collection());
        if (ct == null) throw new IllegalArgumentException(e.typeName() + " has no collection " + in.collection());
        boolean present = ct.current().contains(in.element());
        if (present == in.added()) return new RecordOccurrenceOut(e.getId(), 0);
        Occurrence occ = occurrence(agent, e, in, () -> causes.resolve(name, in.cause()));
        if (in.added()) ct.add(in.element(), occ);
        else ct.remove(in.element(), occ);
        CollectionChange<Object> change = new CollectionChange<>(in.element(),
                in.added() ? CollectionChange.Operation.ADDED : CollectionChange.Operation.REMOVED);
        historyRepository.appendOccurrence(name, new OccurrenceRecord(e, OccurrenceRecord.Kind.CHANGE, in.collection(), change, occ));
        return new RecordOccurrenceOut(e.getId(), occ.sequence());
    }

    private RecordOccurrenceOut arrival(Agent agent, RecordOccurrenceIn.Arrival in) {
        String name = agent.getName();
        Message m = (Message) causes.entity(name, in.messageId());
        Occurrence occ = occurrence(agent, m, in, () -> null);
        m.arrive(occ);
        historyRepository.appendOccurrence(name, new OccurrenceRecord(m, OccurrenceRecord.Kind.ARRIVAL, null, null, occ));
        return new RecordOccurrenceOut(m.getId(), occ.sequence());
    }

    private RecordOccurrenceOut addBinding(Agent agent, RecordOccurrenceIn.AddBinding in) {
        IntendedMeans im = (IntendedMeans) causes.entity(agent.getName(), in.intendedMeansId());
        long seq = sequenceProvider.next(agent.getName());
        lastSequence.put(agent.getName(), seq);
        im.addBinding(new Binding(in.name(), in.value(), in.moment(), seq));
        return new RecordOccurrenceOut(im.getId(), seq);
    }

    private RecordOccurrenceOut end(Agent agent, RecordOccurrenceIn.End in) {
        String name = agent.getName();
        Entity<?> e = causes.entity(name, in.entityId());
        Occurrence occ = occurrence(agent, e, in, () -> causes.resolve(name, in.cause()));
        e.finish(occ);
        historyRepository.appendOccurrence(name, new OccurrenceRecord(e, OccurrenceRecord.Kind.END, null, null, occ));
        return new RecordOccurrenceOut(e.getId(), occ.sequence());
    }
}
