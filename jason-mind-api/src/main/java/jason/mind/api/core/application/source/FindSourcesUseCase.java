package jason.mind.api.core.application.source;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jason.mind.api.core.application.history.HistoryView;
import jason.mind.api.core.domain.model.entities.Plan;
import jason.mind.api.core.domain.model.program.SourceFile;
import jason.mind.api.core.domain.model.timeline.Entity;
import jason.mind.api.core.ports.in.source.FindSourcesIn;
import jason.mind.api.core.ports.in.source.FindSourcesPort;
import jason.mind.api.core.ports.in.source.SourcesOut;
import jason.mind.api.core.ports.out.history.HistoryQueryRepositoryPort;
import jason.mind.api.core.ports.out.source.SourceRepositoryPort;

public class FindSourcesUseCase implements FindSourcesPort {

    private final SourceRepositoryPort sourceRepository;
    private final HistoryQueryRepositoryPort historyQuery;

    public FindSourcesUseCase(SourceRepositoryPort sourceRepository, HistoryQueryRepositoryPort historyQuery) {
        this.sourceRepository = sourceRepository;
        this.historyQuery = historyQuery;
    }

    @Override
    public SourcesOut execute(FindSourcesIn in) {
        HistoryView.agent(historyQuery, in.agent());
        long cut = HistoryView.lastCut(historyQuery, in.agent());
        Set<SourceFile> cited = new LinkedHashSet<>();
        for (Entity<?> e : historyQuery.entitiesOfTypes(in.agent(), Set.of("plan"))) {
            Plan p = (Plan) e;
            if (p.getStart().sequence() > cut || p.getSource() == null || p.getSource().file() == null) continue;
            cited.add(p.getSource().file());
        }
        List<SourcesOut.FileOut> files = new ArrayList<>();
        for (SourceFile f : sourceRepository.sourcesOf(in.agent()))
            if (cited.remove(f)) files.add(new SourcesOut.FileOut(f.fileName(), f.content()));
        for (SourceFile f : cited) files.add(new SourcesOut.FileOut(f.fileName(), f.content()));
        return new SourcesOut(in.agent(), files);
    }
}
