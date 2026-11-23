package jason.mind.api.core.ports.in.console;

import java.util.List;

public record ConsoleOut(List<LineOut> lines, Long previousCursor, Long nextCursor) {

    public record LineOut(String agent, int cycle, long sequence, long timeMilliseconds, String text) {
    }
}
