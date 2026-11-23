package jason.mind.api.core.application.capture;

import jason.mind.api.core.domain.model.history.Agent;
import jason.mind.api.core.domain.model.timeline.Occurrence;
import jason.mind.api.core.ports.in.capture.StartAgentIn;
import jason.mind.api.core.ports.in.capture.StartAgentPort;
import jason.mind.api.core.ports.out.history.HistoryRepositoryPort;
import jason.mind.api.core.ports.out.sequence.SequenceProviderPort;

public class StartAgentUseCase implements StartAgentPort {

    private final HistoryRepositoryPort historyRepository;
    private final SequenceProviderPort sequenceProvider;
    private final CauseResolver causes;

    public StartAgentUseCase(HistoryRepositoryPort historyRepository, SequenceProviderPort sequenceProvider) {
        this.historyRepository = historyRepository;
        this.sequenceProvider = sequenceProvider;
        this.causes = new CauseResolver(historyRepository);
    }

    @Override
    public void execute(StartAgentIn in) {
        Occurrence start = null;
        if (in.parentMoment() != null) {
            String parent = in.parentMoment().agent();
            start = new Occurrence(in.parentMoment(), sequenceProvider.next(parent), causes.resolve(parent, in.parentCause()));
        }
        historyRepository.saveAgent(new Agent(in.name(), start, in.config()));
    }
}
