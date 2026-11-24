package jason.mind.api.core.application.capture;

import jason.mind.api.core.domain.model.history.Agent;
import jason.mind.api.core.domain.model.timeline.Occurrence;
import jason.mind.api.core.ports.in.capture.StartAgentIn;
import jason.mind.api.core.ports.in.capture.StartAgentPort;
import jason.mind.api.core.ports.out.history.HistoryRepositoryPort;
import jason.mind.api.core.ports.out.sequence.SequenceProviderPort;

/**
 * Use case that records the start of an agent.
 * When the agent was created by another agent, its start occurrence is stamped in the parent's
 * sequence with the cause reported by the parent.
 *
 * @author Gabriel Lima
 */
public class StartAgentUseCase implements StartAgentPort {

    /** History where the agent is saved. */
    private final HistoryRepositoryPort historyRepository;

    /** Source of the sequence number of the parent agent. */
    private final SequenceProviderPort sequenceProvider;

    /** Resolver of the cause reported by the parent agent. */
    private final CauseResolver causes;

    /**
     * Creates the use case with the history and the sequence provider.
     *
     * @param historyRepository {@link #historyRepository}
     * @param sequenceProvider  {@link #sequenceProvider}
     */
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
            start = new Occurrence(in.parentMoment(), this.sequenceProvider.next(parent),
                    this.causes.resolve(parent, in.parentCause()));
        }
        this.historyRepository.saveAgent(new Agent(in.name(), start, in.config()));
    }
}
