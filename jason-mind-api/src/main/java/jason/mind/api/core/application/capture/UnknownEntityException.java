package jason.mind.api.core.application.capture;

public class UnknownEntityException extends RuntimeException {

    public UnknownEntityException(String agent, long id) {
        super("entity " + id + " unknown in agent " + agent);
    }
}
