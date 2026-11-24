package jason.mind.api.infrastructure.adapters.out.metrics;

import jason.mind.api.core.ports.out.metrics.RuntimeMemoryPort;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

/**
 * Reads the heap of the JVM from the {@link MemoryMXBean}.
 *
 * @author Gabriel Lima
 */
public class RuntimeMemoryAdapter implements RuntimeMemoryPort {

    /** Memory bean of the JVM. */
    private final MemoryMXBean bean = ManagementFactory.getMemoryMXBean();

    @Override
    public long heapUsedBytes() {
        return this.bean.getHeapMemoryUsage().getUsed();
    }

    @Override
    public long heapMaxBytes() {
        return this.bean.getHeapMemoryUsage().getMax();
    }
}
