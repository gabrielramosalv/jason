package jason.mind.api.core.domain.model.timeline;

import jason.mind.api.core.domain.model.entities.Plan;

public record CodeLocation(Plan plan, Integer deed) {

    public Integer line() {
        if (plan == null || deed == null || deed < 0 || deed >= plan.getBody().size()) return null;
        return plan.getBody().get(deed).line();
    }
}
