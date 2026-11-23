package jason.mind.api.core.application.history;

public class CyclePrunedException extends RuntimeException {

    public CyclePrunedException(String agent, int cycle) {
        super("cycle " + cycle + " of " + agent + " was pruned");
    }
}
