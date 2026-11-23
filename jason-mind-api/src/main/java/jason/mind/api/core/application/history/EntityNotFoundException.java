package jason.mind.api.core.application.history;

public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(String agent, String id) {
        super("entity " + id + " not found in " + agent);
    }
}
