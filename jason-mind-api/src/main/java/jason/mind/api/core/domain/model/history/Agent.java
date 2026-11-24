package jason.mind.api.core.domain.model.history;

import jason.mind.api.core.domain.model.timeline.Occurrence;
import lombok.Getter;

import java.util.Objects;

/**
 * Agent whose history is recorded.
 * Keeps its name, start, configuration and, once finished, its end.
 *
 * @author Gabriel Lima
 */
@Getter
public final class Agent {

    /** Name of the agent. */
    private final String name;

    /** Occurrence that started the agent. */
    private final Occurrence start;

    /** Configuration of the agent. */
    private final AgentConfig config;

    /** Occurrence that ended the agent, or null while alive. */
    private volatile Occurrence end;

    /**
     * Creates the agent with its name, start occurrence and configuration.
     *
     * @param name   {@link #name}
     * @param start  {@link #start}
     * @param config {@link #config}
     * @throws NullPointerException if the name or the configuration is null
     */
    public Agent(String name, Occurrence start, AgentConfig config) {
        this.name = Objects.requireNonNull(name, "name");
        this.start = start;
        this.config = Objects.requireNonNull(config, "config");
    }

    /**
     * Tells whether the agent has ended.
     *
     * @return true if an end was recorded
     */
    public boolean hasEnded() {
        return this.end != null;
    }

    /**
     * Records the end of the agent.
     *
     * @param end occurrence that ends the agent
     * @throws NullPointerException     if the end is null
     * @throws IllegalArgumentException if the end belongs to another agent
     * @throws IllegalStateException    if the agent already ended
     */
    public void finish(Occurrence end) {
        Objects.requireNonNull(end, "end");
        if (!end.agent().equals(this.name)) {
            throw new IllegalArgumentException("end outside the agent history");
        }
        if (this.end != null) {
            throw new IllegalStateException("agent " + this.name + " already ended");
        }
        this.end = end;
    }

}
