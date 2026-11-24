package jason.mind.api.core.application.source;

import jason.mind.api.core.application.history.HistoryView;
import jason.mind.api.core.domain.model.program.SourceFile;
import jason.mind.api.core.ports.in.source.FindSourcesIn;
import jason.mind.api.core.ports.in.source.FindSourcesPort;
import jason.mind.api.core.ports.in.source.SourcesOut;
import jason.mind.api.core.ports.out.history.HistoryQueryRepositoryPort;
import jason.mind.api.core.ports.out.source.SourceRepositoryPort;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Use case that lists the source files registered for an agent, in registration order.
 *
 * @author Gabriel Lima
 */
@RequiredArgsConstructor
public class FindSourcesUseCase implements FindSourcesPort {

    /** Repository that holds the registered source files. */
    private final SourceRepositoryPort sourceRepository;

    /** Read-only access to the history, used to check that the agent exists. */
    private final HistoryQueryRepositoryPort historyQuery;

    @Override
    public SourcesOut execute(FindSourcesIn in) {
        HistoryView.agent(this.historyQuery, in.agent());
        List<SourcesOut.FileOut> files = new ArrayList<>();
        for (SourceFile f : this.sourceRepository.sourcesOf(in.agent())) {
            files.add(new SourcesOut.FileOut(f.fileName(), f.content()));
        }
        return new SourcesOut(in.agent(), files);
    }
}
