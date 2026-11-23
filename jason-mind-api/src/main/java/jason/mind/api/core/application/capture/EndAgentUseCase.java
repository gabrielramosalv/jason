package jason.mind.api.core.application.capture;

import jason.mind.api.core.domain.model.history.Agent;
import jason.mind.api.core.domain.model.timeline.Moment;
import jason.mind.api.core.domain.model.timeline.Occurrence;
import jason.mind.api.core.domain.model.timeline.ReasoningStep;
import jason.mind.api.core.ports.in.capture.EndAgentIn;
import jason.mind.api.core.ports.in.capture.EndAgentPort;
import jason.mind.api.core.ports.out.history.HistoryRepositoryPort;
import jason.mind.api.core.ports.out.sequence.SequenceProviderPort;

public class EndAgentUseCase implements EndAgentPort {

    private final HistoryRepositoryPort historyRepository;
    private final SequenceProviderPort sequenceProvider;

    public EndAgentUseCase(HistoryRepositoryPort historyRepository, SequenceProviderPort sequenceProvider) {
        this.historyRepository = historyRepository;
        this.sequenceProvider = sequenceProvider;
    }

    @Override
    public void execute(EndAgentIn in) {
        Agent agent = historyRepository.loadAgent(in.name()).orElseThrow(() -> new UnknownAgentException(in.name()));
        if (agent.hasEnded()) return;
        Moment m = Moment.of(in.name(), Math.max(0, in.lastCycle()), 0, ReasoningStep.OUT_OF_CYCLE, 1);
        agent.finish(new Occurrence(m, sequenceProvider.next(in.name()), null));
        historyRepository.saveAgent(agent);
    }
}
