package jason.mind.api.core.application.plan;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import jason.mind.api.core.application.history.EntityIds;
import jason.mind.api.core.application.history.HistoryView;
import jason.mind.api.core.domain.model.entities.Plan;
import jason.mind.api.core.domain.model.program.Annotation;
import jason.mind.api.core.domain.model.program.Deed;
import jason.mind.api.core.domain.model.program.Source;
import jason.mind.api.core.domain.model.timeline.Entity;
import jason.mind.api.core.ports.in.plan.FindPlansIn;
import jason.mind.api.core.ports.in.plan.FindPlansPort;
import jason.mind.api.core.ports.in.plan.PlansOut;
import jason.mind.api.core.ports.out.history.HistoryQueryRepositoryPort;

public class FindPlansUseCase implements FindPlansPort {

    private final HistoryQueryRepositoryPort historyQuery;

    public FindPlansUseCase(HistoryQueryRepositoryPort historyQuery) {
        this.historyQuery = historyQuery;
    }

    @Override
    public PlansOut execute(FindPlansIn in) {
        HistoryView.agent(historyQuery, in.agent());
        long cut = HistoryView.lastCut(historyQuery, in.agent());
        List<Plan> plans = new ArrayList<>();
        long version = 0;
        for (Entity<?> e : historyQuery.entitiesOfTypes(in.agent(), Set.of("plan"))) {
            if (e.getStart().sequence() > cut) continue;
            plans.add((Plan) e);
            version = Math.max(version, e.getStart().sequence());
            if (e.getEnd() != null && e.getEnd().sequence() <= cut) version = Math.max(version, e.getEnd().sequence());
        }
        plans.sort(Comparator.comparingLong(Plan::getOrderKey));
        List<PlansOut.PlanOut> out = new ArrayList<>();
        for (Plan p : plans) {
            List<String> annotations = new ArrayList<>();
            for (Annotation a : p.getAnnotations()) annotations.add(a.term());
            for (Source s : p.getSources().elementsBefore(cut + 1)) annotations.add(s.toAnnotation().term());
            List<PlansOut.DeedOut> body = new ArrayList<>();
            for (Deed d : p.getBody()) body.add(new PlansOut.DeedOut(d.type().name(), d.content(), d.line(), d.parent()));
            PlansOut.SourceOut source = p.getSource() == null ? null
                    : new PlansOut.SourceOut(p.getSource().file() == null ? "" : p.getSource().file().fileName(), p.getSource().line());
            Integer toCycle = p.getEnd() != null && p.getEnd().sequence() <= cut ? p.getEnd().cycle() : null;
            out.add(new PlansOut.PlanOut(EntityIds.format(p), p.getLabel(),
                    new PlansOut.TriggerOut(p.getTrigger().operator().symbol(), p.getTrigger().type().name(), p.getTrigger().literal()),
                    p.getContext(), source, annotations, p.getOrderKey(), new PlansOut.Alive(p.getStart().cycle(), toCycle), body));
        }
        return new PlansOut(in.agent(), version, out);
    }
}
