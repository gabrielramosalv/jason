package jason.mind.api.infrastructure.adapters.in.jason.capture;

import jason.asSemantics.Event;
import jason.asSemantics.Message;
import jason.asSemantics.ReasoningListener;
import jason.asSemantics.TransitionSystem;
import jason.asSyntax.Plan;
import jason.mind.api.infrastructure.metrics.MindTiming;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Reasoning listener that times every call to the capture of one agent, used only when
 * {@link MindTiming#TIMING} is on.
 *
 * @author Gabriel Lima
 */
@RequiredArgsConstructor
final class TimedReasoningListener implements ReasoningListener {

    /** Listener that captures the reasoning of the agent. */
    private final ReasoningListener delegate;

    /** Name of the agent whose capture is timed. */
    private final String agent;

    /**
     * Runs one call to the capture and adds its duration to the agent.
     *
     * @param call call to the capture
     */
    private void timed(Runnable call) {
        long t0 = System.nanoTime();
        try {
            call.run();
        } finally {
            MindTiming.addCapture(this.agent, System.nanoTime() - t0);
        }
    }

    @Override
    public void cycleStarting(TransitionSystem ts, int cycle) {
        this.timed(() -> this.delegate.cycleStarting(ts, cycle));
    }

    @Override
    public void cycleFinished(TransitionSystem ts, int cycle) {
        this.timed(() -> this.delegate.cycleFinished(ts, cycle));
    }

    @Override
    public void phaseStarting(TransitionSystem ts, Phase phase) {
        this.timed(() -> this.delegate.phaseStarting(ts, phase));
    }

    @Override
    public void stepStarting(TransitionSystem ts, Step step) {
        this.timed(() -> this.delegate.stepStarting(ts, step));
    }

    @Override
    public void stepFinished(TransitionSystem ts, Step step) {
        this.timed(() -> this.delegate.stepFinished(ts, step));
    }

    @Override
    public void planSearchStarting(TransitionSystem ts, Event event, SearchMethod method) {
        this.timed(() -> this.delegate.planSearchStarting(ts, event, method));
    }

    @Override
    public void planEvaluated(TransitionSystem ts, Plan plan, boolean relevant, boolean applicable,
                              List<TermResult> terms) {
        this.timed(() -> this.delegate.planEvaluated(ts, plan, relevant, applicable, terms));
    }

    @Override
    public void messageSent(TransitionSystem ts, Message m) {
        this.timed(() -> this.delegate.messageSent(ts, m));
    }
}
