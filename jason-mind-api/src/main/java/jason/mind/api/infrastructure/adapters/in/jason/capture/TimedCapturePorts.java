package jason.mind.api.infrastructure.adapters.in.jason.capture;

import jason.mind.api.infrastructure.metrics.MindTiming;

import java.util.function.Supplier;

/**
 * Factory of capture ports that time the recording into the history, used only when {@link MindTiming#TIMING} is on.
 *
 * @author Gabriel Lima
 */
public final class TimedCapturePorts {

    /**
     * Prevents instantiation of this utility class.
     */
    private TimedCapturePorts() {
    }

    /**
     * Wraps the capture ports so that every call adds its duration to the recording time of its agent.
     * Returns the ports unchanged when {@link MindTiming#TIMING} is off.
     *
     * @param p ports to wrap
     * @return ports that time each call, or the given ports when timing is off
     */
    public static CapturePorts wrap(CapturePorts p) {
        if (!MindTiming.TIMING) {
            return p;
        }
        return new CapturePorts(
                in -> time(in.name(), run(() -> p.startMas().execute(in))),
                in -> time(in.name(), run(() -> p.startAgent().execute(in))),
                in -> time(in.name(), run(() -> p.endAgent().execute(in))),
                in -> time(in.agent(), run(() -> p.loadSources().execute(in))),
                in -> time(in.agent(), () -> p.recordOccurrence().execute(in)),
                in -> time(in.agent(), run(() -> p.recordPlanSelection().execute(in))),
                in -> time(in.agent(), run(() -> p.finishCycle().execute(in))),
                in -> time(in.agent(), run(() -> p.recordConsoleLine().execute(in))));
    }

    /**
     * Runs a call and adds its duration to the recording time of an agent.
     *
     * @param agent name of the agent
     * @param call  call to the port
     * @param <T>   type of the result
     * @return result of the call
     */
    private static <T> T time(String agent, Supplier<T> call) {
        long t0 = System.nanoTime();
        try {
            return call.get();
        } finally {
            MindTiming.addRecording(agent, System.nanoTime() - t0);
        }
    }

    /**
     * Adapts a call without result to a supplier.
     *
     * @param call call to run
     * @return supplier that runs the call and returns {@code null}
     */
    private static Supplier<Void> run(Runnable call) {
        return () -> {
            call.run();
            return null;
        };
    }
}
