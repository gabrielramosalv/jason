package jason.mind.api.core.application.capture;

import jason.mind.api.core.ports.in.capture.StartMasIn;
import jason.mind.api.core.ports.in.capture.StartMasPort;
import jason.mind.api.core.ports.out.mas.MasRepositoryPort;
import lombok.RequiredArgsConstructor;

/**
 * Use case that records the start of the multi-agent system by saving its name.
 *
 * @author Gabriel Lima
 */
@RequiredArgsConstructor
public class StartMasUseCase implements StartMasPort {

    /** Repository where the system name is saved. */
    private final MasRepositoryPort masRepository;

    @Override
    public void execute(StartMasIn in) {
        this.masRepository.save(in.name());
    }
}
