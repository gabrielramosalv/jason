package jason.mind.api.core.ports.out.metrics;

/**
 * Port that reads the memory of the JVM that runs the multi-agent system.
 *
 * @author Gabriel Lima
 */
public interface RuntimeMemoryPort {

    /**
     * Returns the heap in use.
     *
     * @return used heap in bytes, shared by Jason, the Mind and the user's application
     */
    long heapUsedBytes();

    /**
     * Returns the largest heap the JVM may use.
     *
     * @return maximum heap in bytes, or {@code -1} when it is not defined
     */
    long heapMaxBytes();
}
