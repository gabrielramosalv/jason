package jason.mind.api.core.domain.model.console;

public record ConsoleLine(String agent, int cycle, long sequence, long timeMilliseconds, String text) {
}
