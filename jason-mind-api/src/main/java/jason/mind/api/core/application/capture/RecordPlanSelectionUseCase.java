package jason.mind.api.core.application.capture;

import java.util.ArrayList;
import java.util.List;

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

public class RecordPlanSelectionUseCase implements RecordPlanSelectionPort {

    private final HistoryRepositoryPort historyRepository;
    private final SequenceProviderPort sequenceProvider;
    private final CauseResolver entities;

    public RecordPlanSelectionUseCase(HistoryRepositoryPort historyRepository, SequenceProviderPort sequenceProvider) {
        this.historyRepository = historyRepository;
        this.sequenceProvider = sequenceProvider;
        this.entities = new CauseResolver(historyRepository);
    }

    @Override
    public void execute(RecordPlanSelectionIn in) {
        String agent = in.agent();
        long seq = sequenceProvider.next(agent);
        List<PlanEvaluation> evaluations = new ArrayList<>();
        for (RecordPlanSelectionIn.Evaluation ev : in.evaluations()) {
            Plan plan = (Plan) entities.entity(agent, ev.planId());
            List<TermEvaluation> terms = new ArrayList<>();
            for (RecordPlanSelectionIn.Term t : ev.terms()) {
                List<Binding> bindings = new ArrayList<>();
                for (String[] b : t.bindings()) bindings.add(new Binding(b[0], b[1], in.moment(), seq));
                terms.add(new TermEvaluation(t.index(), t.verdict(), bindings));
            }
            evaluations.add(new PlanEvaluation(plan, terms));
        }
        Event event = (Event) entities.entity(agent, in.eventId());
        historyRepository.appendPlanSelection(agent, new PlanSelection(in.moment(), seq, event, in.searchMethod(), evaluations));
    }
}
