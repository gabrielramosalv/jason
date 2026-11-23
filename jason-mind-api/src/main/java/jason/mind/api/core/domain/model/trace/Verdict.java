package jason.mind.api.core.domain.model.trace;

public enum Verdict {
    TRUE, FALSE, NOT_CHECKED, MIXED;

    public static Verdict of(int attempts, int successes) {
        if (attempts == 0) return NOT_CHECKED;
        if (successes == attempts) return TRUE;
        if (successes == 0) return FALSE;
        return MIXED;
    }
}
