package jason.mind.api.core.application.capture;

import jason.mind.api.core.ports.in.capture.StartMasIn;
import jason.mind.api.core.ports.in.capture.StartMasPort;
import jason.mind.api.core.ports.out.mas.MasRepositoryPort;

public class StartMasUseCase implements StartMasPort {

    private final MasRepositoryPort masRepository;

    public StartMasUseCase(MasRepositoryPort masRepository) {
        this.masRepository = masRepository;
    }

    @Override
    public void execute(StartMasIn in) {
        masRepository.save(in.name());
    }
}
