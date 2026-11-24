package jason.mind.api.core.application.capture;

import jason.mind.api.core.domain.model.history.Agent;
import jason.mind.api.core.domain.model.timeline.Moment;
import jason.mind.api.core.domain.model.timeline.Occurrence;
import jason.mind.api.core.domain.model.timeline.ReasoningStep;
import jason.mind.api.core.ports.in.capture.EndAgentIn;
import jason.mind.api.core.ports.in.capture.EndAgentPort;
import jason.mind.api.core.ports.out.history.HistoryRepositoryPort;
import jason.mind.api.core.ports.out.sequence.SequenceProviderPort;
import lombok.RequiredArgsConstructor;

/**
 * Use case that records the end of an agent.
 * The end is stamped out of cycle, after the last cycle reported, and is recorded only once.
 *
 * @author Gabriel Lima
 */
@RequiredArgsConstructor
public class EndAgentUseCase implements EndAgentPort {

    /** History where the agent is loaded and saved. */
    private final HistoryRepositoryPort historyRepository;

    /** Source of the sequence numbers of the agent. */
    private final SequenceProviderPort sequenceProvider;

    @Override
    public void execute(EndAgentIn in) {
        Agent agent = this.historyRepository.loadAgent(in.name()).orElseThrow(
                () -> new UnknownAgentException(in.name()));
        if (agent.hasEnded()) {
            return;
        }
        Moment m = Moment.of(in.name(), Math.max(0, in.lastCycle()), 0, ReasoningStep.OUT_OF_CYCLE, 1);
        agent.finish(new Occurrence(m, this.sequenceProvider.next(in.name()), null));
        this.historyRepository.saveAgent(agent);
    }
}
