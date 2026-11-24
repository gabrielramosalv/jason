package jason.mind.api.core.domain.model.trace;

/**
 * Verdict of the evaluation of a context term.
 *
 * @author Gabriel Lima
 */
public enum Verdict {
    /** Every attempt succeeded. */
    TRUE,
    /** Every attempt failed. */
    FALSE,
    /** The term was not evaluated. */
    NOT_CHECKED,
    /** Some attempts succeeded and some failed. */
    MIXED;

    /**
     * Derives the verdict from the number of attempts and successes.
     *
     * @param attempts  number of evaluation attempts
     * @param successes number of successful attempts
     * @return {@code NOT_CHECKED} without attempts, {@code TRUE} if all succeeded, {@code FALSE} if none did,
     * otherwise {@code MIXED}
     */
    public static Verdict of(int attempts, int successes) {
        if (attempts == 0) {
            return NOT_CHECKED;
        }
        if (successes == attempts) {
            return TRUE;
        }
        if (successes == 0) {
            return FALSE;
        }
        return MIXED;
    }
}
