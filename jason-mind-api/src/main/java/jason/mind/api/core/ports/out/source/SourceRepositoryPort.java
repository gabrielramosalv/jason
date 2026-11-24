package jason.mind.api.core.ports.out.source;

import jason.mind.api.core.domain.model.program.SourceFile;

import java.util.List;
import java.util.Optional;

/**
 * Port for storing the source files of the agents.
 *
 * @author Gabriel Lima
 */
public interface SourceRepositoryPort {

    /**
     * Lists the source files of an agent.
     *
     * @param agentName name of the agent
     * @return source files of the agent
     */
    List<SourceFile> sourcesOf(String agentName);

    /**
     * Finds a source file by path.
     *
     * @param path path of the file
     * @return file, or empty when unknown
     */
    Optional<SourceFile> sourceAt(String path);

    /**
     * Registers the source files of an agent.
     *
     * @param agentName name of the agent
     * @param files     source files to register
     */
    void register(String agentName, List<SourceFile> files);
}
