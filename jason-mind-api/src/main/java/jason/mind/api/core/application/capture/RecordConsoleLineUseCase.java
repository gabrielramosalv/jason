package jason.mind.api.core.application.capture;

import jason.mind.api.core.domain.model.console.ConsoleLine;
import jason.mind.api.core.ports.in.capture.RecordConsoleLineIn;
import jason.mind.api.core.ports.in.capture.RecordConsoleLinePort;
import jason.mind.api.core.ports.out.clock.ClockProviderPort;
import jason.mind.api.core.ports.out.console.ConsoleRepositoryPort;
import jason.mind.api.core.ports.out.sequence.SequenceProviderPort;

public class RecordConsoleLineUseCase implements RecordConsoleLinePort {

    private final ConsoleRepositoryPort consoleRepository;
    private final ClockProviderPort clockProvider;
    private final SequenceProviderPort sequenceProvider;

    public RecordConsoleLineUseCase(ConsoleRepositoryPort consoleRepository, ClockProviderPort clockProvider,
                                    SequenceProviderPort sequenceProvider) {
        this.consoleRepository = consoleRepository;
        this.clockProvider = clockProvider;
        this.sequenceProvider = sequenceProvider;
    }

    @Override
    public void execute(RecordConsoleLineIn in) {
        synchronized (consoleRepository) {
            consoleRepository.append(new ConsoleLine(in.agent(), in.cycle(), sequenceProvider.next(in.agent()),
                    clockProvider.nowMilliseconds(), in.text()));
        }
    }
}
