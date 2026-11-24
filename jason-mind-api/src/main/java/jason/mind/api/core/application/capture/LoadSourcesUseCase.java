package jason.mind.api.core.application.capture;

import jason.mind.api.core.ports.in.capture.LoadSourcesIn;
import jason.mind.api.core.ports.in.capture.LoadSourcesPort;
import jason.mind.api.core.ports.out.source.SourceRepositoryPort;
import lombok.RequiredArgsConstructor;

/**
 * Use case that registers the source files of an agent.
 *
 * @author Gabriel Lima
 */
@RequiredArgsConstructor
public class LoadSourcesUseCase implements LoadSourcesPort {

    /** Repository where the source files are registered. */
    private final SourceRepositoryPort sourceRepository;

    @Override
    public void execute(LoadSourcesIn in) {
        this.sourceRepository.register(in.agent(), in.files());
    }
}
