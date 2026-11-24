package jason.mind.api.core.ports.in.console;

import java.util.List;

/**
 * Output with a page of console lines.
 *
 * @param lines          lines of the page
 * @param previousCursor cursor of the previous page, or null
 * @param nextCursor     cursor of the next page, or null
 * @author Gabriel Lima
 */
public record ConsoleOut(List<LineOut> lines, Long previousCursor, Long nextCursor) {

    /**
     * Console line.
     *
     * @param agent            name of the agent
     * @param cycle            cycle of the print
     * @param sequence         global sequence number of the line
     * @param timeMilliseconds wall-clock time of the print, in milliseconds
     * @param text             printed text
     */
    public record LineOut(String agent, int cycle, long sequence, long timeMilliseconds, String text) {

    }
}
