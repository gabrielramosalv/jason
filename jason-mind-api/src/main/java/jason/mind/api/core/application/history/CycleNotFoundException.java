package jason.mind.api.core.application.history;

public class CycleNotFoundException extends RuntimeException {

    public CycleNotFoundException(String agent, int cycle) {
        super("cycle " + cycle + " of " + agent + " is not closed");
    }
}
