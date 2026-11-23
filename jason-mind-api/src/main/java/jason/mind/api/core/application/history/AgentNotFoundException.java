package jason.mind.api.core.application.history;

public class AgentNotFoundException extends RuntimeException {

    public AgentNotFoundException(String agent) {
        super("agent not found: " + agent);
    }
}
