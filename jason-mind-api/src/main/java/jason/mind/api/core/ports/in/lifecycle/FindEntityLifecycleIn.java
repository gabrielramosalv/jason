package jason.mind.api.core.ports.in.lifecycle;

public record FindEntityLifecycleIn(String agent, String id, Long after, Integer limit) {
}
