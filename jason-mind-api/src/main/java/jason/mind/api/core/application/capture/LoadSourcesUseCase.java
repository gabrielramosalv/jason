package jason.mind.api.core.application.capture;

import jason.mind.api.core.ports.in.capture.LoadSourcesIn;
import jason.mind.api.core.ports.in.capture.LoadSourcesPort;
import jason.mind.api.core.ports.out.source.SourceRepositoryPort;

public class LoadSourcesUseCase implements LoadSourcesPort {

    private final SourceRepositoryPort sourceRepository;

    public LoadSourcesUseCase(SourceRepositoryPort sourceRepository) {
        this.sourceRepository = sourceRepository;
    }

    @Override
    public void execute(LoadSourcesIn in) {
        sourceRepository.register(in.agent(), in.files());
    }
}
