package jason.mind.api.core.application.history;

public class EntityPrunedException extends RuntimeException {

    public EntityPrunedException(String agent, String id) {
        super("entity " + id + " of " + agent + " was pruned");
    }
}
