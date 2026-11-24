package jason.mind.api.core.application.capture;

import jason.mind.api.core.domain.model.history.CycleRecord;
import jason.mind.api.core.ports.in.capture.FinishCycleIn;
import jason.mind.api.core.ports.in.capture.FinishCyclePort;
import jason.mind.api.core.ports.out.clock.ClockProviderPort;
import jason.mind.api.core.ports.out.console.ConsoleRepositoryPort;
import jason.mind.api.core.ports.out.history.HistoryRepositoryPort;
import jason.mind.api.core.ports.out.sequence.SequenceProviderPort;
import jason.mind.api.core.ports.out.settings.SettingsRepositoryPort;
import lombok.RequiredArgsConstructor;

/**
 * Use case that closes a reasoning cycle of an agent.
 * It commits the cycle record and prunes the history and console lines that fall behind the retention horizon.
 *
 * @author Gabriel Lima
 */
@RequiredArgsConstructor
public class FinishCycleUseCase implements FinishCyclePort {

    /** History where the cycle is committed and pruned. */
    private final HistoryRepositoryPort historyRepository;

    /** Source of the wall-clock time stamped on the cycle. */
    private final ClockProviderPort clockProvider;

    /** Source of the sequence number that closes the cycle. */
    private final SequenceProviderPort sequenceProvider;

    /** Console lines pruned together with the history. */
    private final ConsoleRepositoryPort consoleRepository;

    /** Settings that define how many cycles are retained. */
    private final SettingsRepositoryPort settingsRepository;

    @Override
    public void execute(FinishCycleIn in) {
        long sequenceEnd = this.sequenceProvider.next(in.agent());
        this.historyRepository.commitCycle(in.agent(),
                new CycleRecord(in.cycle(), this.clockProvider.nowMilliseconds(), sequenceEnd, in.sense(),
                        in.deliberate(), in.act()));
        int horizon = in.cycle() - this.settingsRepository.load().retentionCycles() + 1;
        if (horizon > 0) {
            this.historyRepository.prune(in.agent(), horizon);
            this.consoleRepository.pruneBefore(in.agent(), horizon);
        }
    }
}
