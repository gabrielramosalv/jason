package jason.mind.api.core.application.capture;

import jason.mind.api.core.domain.model.history.CycleRecord;
import jason.mind.api.core.ports.in.capture.FinishCycleIn;
import jason.mind.api.core.ports.in.capture.FinishCyclePort;
import jason.mind.api.core.ports.out.clock.ClockProviderPort;
import jason.mind.api.core.ports.out.console.ConsoleRepositoryPort;
import jason.mind.api.core.ports.out.history.HistoryRepositoryPort;
import jason.mind.api.core.ports.out.sequence.SequenceProviderPort;
import jason.mind.api.core.ports.out.settings.SettingsRepositoryPort;

public class FinishCycleUseCase implements FinishCyclePort {

    private final HistoryRepositoryPort historyRepository;
    private final ClockProviderPort clockProvider;
    private final SequenceProviderPort sequenceProvider;
    private final ConsoleRepositoryPort consoleRepository;
    private final SettingsRepositoryPort settingsRepository;

    public FinishCycleUseCase(HistoryRepositoryPort historyRepository, ClockProviderPort clockProvider,
                              SequenceProviderPort sequenceProvider, ConsoleRepositoryPort consoleRepository,
                              SettingsRepositoryPort settingsRepository) {
        this.historyRepository = historyRepository;
        this.clockProvider = clockProvider;
        this.sequenceProvider = sequenceProvider;
        this.consoleRepository = consoleRepository;
        this.settingsRepository = settingsRepository;
    }

    @Override
    public void execute(FinishCycleIn in) {
        long sequenceEnd = sequenceProvider.next(in.agent());
        historyRepository.commitCycle(in.agent(), new CycleRecord(in.cycle(), clockProvider.nowMilliseconds(), sequenceEnd,
                in.sense(), in.deliberate(), in.act()));
        int horizon = in.cycle() - settingsRepository.load().retentionCycles() + 1;
        if (horizon > 0) {
            historyRepository.prune(in.agent(), horizon);
            consoleRepository.pruneBefore(in.agent(), horizon);
        }
    }
}
