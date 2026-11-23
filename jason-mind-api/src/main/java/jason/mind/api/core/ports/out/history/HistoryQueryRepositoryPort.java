package jason.mind.api.core.ports.out.history;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jason.mind.api.core.domain.model.entities.Event;
import jason.mind.api.core.domain.model.entities.Message;
import jason.mind.api.core.domain.model.entities.Plan;
import jason.mind.api.core.domain.model.history.Agent;
import jason.mind.api.core.domain.model.history.CycleRecord;
import jason.mind.api.core.domain.model.history.OccurrenceRecord;
import jason.mind.api.core.domain.model.timeline.Entity;
import jason.mind.api.core.domain.model.timeline.Moment;
import jason.mind.api.core.domain.model.trace.PlanSelection;

public interface HistoryQueryRepositoryPort {

    List<Agent> agents();

    Optional<Agent> agent(String name);

    Optional<CycleRecord> lastClosedCycle(String agent);

    Optional<CycleRecord> oldestCycle(String agent);

    Optional<CycleRecord> cycle(String agent, int cycle);

    List<CycleRecord> cycles(String agent, int from, int to);

    List<Entity<?>> entitiesAt(String agent, int cycle);

    List<OccurrenceRecord> occurrencesAt(String agent, int cycle);

    List<OccurrenceRecord> causedBy(String agent, Entity<?> entity);

    List<OccurrenceRecord> occurrencesOf(String agent, Entity<?> entity);

    List<PlanSelection> planSelectionsAt(String agent, int cycle);

    List<PlanSelection> planSelectionsOf(String agent, Event event);

    List<Moment> deedExecutions(String agent, Plan plan, int deedIndex);

    List<Entity<?>> entitiesOfTypes(String agent, Set<String> types);

    Optional<Entity<?>> entityById(String agent, long id);

    boolean wasPruned(String agent, long id);

    Collection<Message> messages();
}
