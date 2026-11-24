package jason.mind.api.core.domain.model.timeline;

import jason.mind.api.core.domain.model.entities.Plan;

/**
 * Location of a deed inside a plan.
 *
 * @param plan plan that contains the deed
 * @param deed index of the deed in the plan body
 * @author Gabriel Lima
 */
public record CodeLocation(Plan plan, Integer deed) {

    /**
     * Returns the source line of the deed.
     *
     * @return line of the deed, or null when the plan, the index or the line is unknown
     */
    public Integer line() {
        if (this.plan == null || this.deed == null || this.deed < 0 || this.deed >= this.plan.getBody().size()) {
            return null;
        }
        return this.plan.getBody().get(this.deed).line();
    }
}
