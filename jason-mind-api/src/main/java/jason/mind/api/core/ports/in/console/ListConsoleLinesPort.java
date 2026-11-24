package jason.mind.api.core.ports.in.console;

/**
 * Port for listing console lines.
 *
 * @author Gabriel Lima
 */
public interface ListConsoleLinesPort {

    /**
     * Lists a page of console lines.
     *
     * @param in filters and pagination
     * @return page of lines with its cursors
     */
    ConsoleOut execute(ListConsoleLinesIn in);
}
