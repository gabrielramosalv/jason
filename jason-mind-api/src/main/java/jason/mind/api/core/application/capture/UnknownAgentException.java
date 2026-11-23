package jason.mind.api.core.application.capture;

public class UnknownAgentException extends RuntimeException {

    public UnknownAgentException(String agent) {
        super("unknown agent: " + agent);
    }
}
