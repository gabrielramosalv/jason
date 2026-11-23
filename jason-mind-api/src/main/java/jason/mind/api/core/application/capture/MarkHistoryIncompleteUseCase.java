package jason.mind.api.core.application.capture;

import jason.mind.api.core.domain.model.history.Agent;
import jason.mind.api.core.ports.in.capture.MarkHistoryIncompleteIn;
import jason.mind.api.core.ports.in.capture.MarkHistoryIncompletePort;
import jason.mind.api.core.ports.out.history.HistoryRepositoryPort;

public class MarkHistoryIncompleteUseCase implements MarkHistoryIncompletePort {

    private final HistoryRepositoryPort historyRepository;

    public MarkHistoryIncompleteUseCase(HistoryRepositoryPort historyRepository) {
        this.historyRepository = historyRepository;
    }

    @Override
    public void execute(MarkHistoryIncompleteIn in) {
        Agent agent = historyRepository.loadAgent(in.name()).orElseThrow(() -> new UnknownAgentException(in.name()));
        agent.markIncompleteSince(in.moment());
        historyRepository.saveAgent(agent);
    }
}
