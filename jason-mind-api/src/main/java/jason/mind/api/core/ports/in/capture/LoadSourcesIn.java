package jason.mind.api.core.ports.in.capture;

import jason.mind.api.core.domain.model.program.SourceFile;

import java.util.List;

/**
 * Input of the source files of an agent.
 *
 * @param agent name of the agent
 * @param files source files of the agent
 * @author Gabriel Lima
 */
public record LoadSourcesIn(String agent, List<SourceFile> files) {

}
