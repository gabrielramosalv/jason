package jason.mind.api.core.application.capture;

import jason.mind.api.core.domain.model.entities.Binding;
import jason.mind.api.core.domain.model.entities.Event;
import jason.mind.api.core.domain.model.entities.Plan;
import jason.mind.api.core.domain.model.trace.PlanEvaluation;
import jason.mind.api.core.domain.model.trace.PlanSelection;
import jason.mind.api.core.domain.model.trace.TermEvaluation;
import jason.mind.api.core.ports.in.capture.RecordPlanSelectionIn;
import jason.mind.api.core.ports.in.capture.RecordPlanSelectionPort;
import jason.mind.api.core.ports.out.history.HistoryRepositoryPort;
import jason.mind.api.core.ports.out.sequence.SequenceProviderPort;

import java.util.ArrayList;
import java.util.List;

/**
 * Use case that records the plan selection made for an event.
 * It stores the evaluation of each candidate plan, with the verdict and bindings of every context term,
 * under a single sequence of the agent.
 *
 * @author Gabriel Lima
 */
public class RecordPlanSelectionUseCase implements RecordPlanSelectionPort {

    /** History where the plan selection is appended. */
    private final HistoryRepositoryPort historyRepository;

    /** Source of the sequence number of the selection. */
    private final SequenceProviderPort sequenceProvider;

    /** Resolver of the plans and the event referenced by the input. */
    private final CauseResolver entities;

    /**
     * Creates the use case with the history and the sequence provider.
     *
     * @param historyRepository {@link #historyRepository}
     * @param sequenceProvider  {@link #sequenceProvider}
     */
    public RecordPlanSelectionUseCase(HistoryRepositoryPort historyRepository, SequenceProviderPort sequenceProvider) {
        this.historyRepository = historyRepository;
        this.sequenceProvider = sequenceProvider;
        this.entities = new CauseResolver(historyRepository);
    }

    @Override
    public void execute(RecordPlanSelectionIn in) {
        String agent = in.agent();
        long seq = this.sequenceProvider.next(agent);
        List<PlanEvaluation> evaluations = new ArrayList<>();
        for (RecordPlanSelectionIn.Evaluation ev : in.evaluations()) {
            Plan plan = (Plan) this.entities.entity(agent, ev.planId());
            List<TermEvaluation> terms = new ArrayList<>();
            for (RecordPlanSelectionIn.Term t : ev.terms()) {
                List<Binding> bindings = new ArrayList<>();
                for (String[] b : t.bindings()) {
                    bindings.add(new Binding(b[0], b[1], in.moment(), seq));
                }
                terms.add(new TermEvaluation(t.index(), t.verdict(), bindings));
            }
            evaluations.add(new PlanEvaluation(plan, terms));
        }
        Event event = (Event) this.entities.entity(agent, in.eventId());
        this.historyRepository.appendPlanSelection(agent,
                new PlanSelection(in.moment(), seq, event, in.searchMethod(), evaluations));
    }
}
