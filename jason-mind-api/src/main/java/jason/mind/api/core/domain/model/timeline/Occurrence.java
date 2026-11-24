package jason.mind.api.core.domain.model.timeline;

import java.util.Objects;

/**
 * Something that happened at a moment, with its global sequence and optional cause.
 *
 * @param moment   moment of the occurrence
 * @param sequence global sequence number of the occurrence
 * @param cause    cause of the occurrence, or null
 * @author Gabriel Lima
 */
public record Occurrence(Moment moment, long sequence, Cause cause) {

    /**
     * Validates the occurrence.
     *
     * @throws NullPointerException if the moment is null
     */
    public Occurrence {
        Objects.requireNonNull(moment, "moment");
    }

    /**
     * Returns the name of the agent.
     *
     * @return agent of the moment
     */
    public String agent() {
        return this.moment.agent();
    }

    /**
     * Returns the cycle number.
     *
     * @return cycle of the moment
     */
    public int cycle() {
        return this.moment.cycle();
    }

    /**
     * Supplier of the cause of an occurrence, computed on demand.
     */
    @FunctionalInterface
    public interface CauseSupplier {

        /**
         * Supplies the cause.
         *
         * @return cause, or null
         */
        Cause get();
    }
}
