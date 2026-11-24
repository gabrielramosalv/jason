package jason.mind.api.core.ports.in.source;

import java.util.List;

/**
 * Output with the source files of an agent.
 *
 * @param agent name of the agent
 * @param files source files
 * @author Gabriel Lima
 */
public record SourcesOut(String agent, List<FileOut> files) {

    /**
     * Source file.
     *
     * @param path    path of the file
     * @param content text of the file
     */
    public record FileOut(String path, String content) {

    }
}
