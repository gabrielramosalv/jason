package jason.mind.api.core.application.capture;

import jason.mind.api.core.domain.model.console.ConsoleLine;
import jason.mind.api.core.ports.in.capture.RecordConsoleLineIn;
import jason.mind.api.core.ports.in.capture.RecordConsoleLinePort;
import jason.mind.api.core.ports.out.clock.ClockProviderPort;
import jason.mind.api.core.ports.out.console.ConsoleRepositoryPort;
import jason.mind.api.core.ports.out.sequence.SequenceProviderPort;
import lombok.RequiredArgsConstructor;

/**
 * Use case that records a line printed to the console by an agent.
 * The sequence number and the append happen under the repository lock, so lines are stored in sequence order.
 *
 * @author Gabriel Lima
 */
@RequiredArgsConstructor
public class RecordConsoleLineUseCase implements RecordConsoleLinePort {

    /** Repository where the console lines are appended. */
    private final ConsoleRepositoryPort consoleRepository;

    /** Source of the wall-clock time stamped on each line. */
    private final ClockProviderPort clockProvider;

    /** Source of the sequence number of each line. */
    private final SequenceProviderPort sequenceProvider;

    @Override
    public void execute(RecordConsoleLineIn in) {
        synchronized (this.consoleRepository) {
            this.consoleRepository.append(
                    new ConsoleLine(in.agent(), in.cycle(), this.sequenceProvider.next(in.agent()),
                            this.clockProvider.nowMilliseconds(), in.text()));
        }
    }
}
