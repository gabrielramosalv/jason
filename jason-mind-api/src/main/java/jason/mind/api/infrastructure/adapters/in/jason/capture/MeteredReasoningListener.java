package jason.mind.api.infrastructure.adapters.in.jason.capture;

import jason.asSemantics.Event;
import jason.asSemantics.Message;
import jason.asSemantics.ReasoningListener;
import jason.asSemantics.TransitionSystem;
import jason.asSyntax.Plan;

import java.util.List;

/**
 * Reasoning listener that adds up the time the capture of one agent spends in a cycle and hands the sum to the
 * adapter just before the cycle is closed, so that it is recorded with the cycle. It costs two clock reads per call.
 * The listener is called only by the thread of the agent, so the sum needs no synchronization.
 *
 * @author Gabriel Lima
 */
final class MeteredReasoningListener implements ReasoningListener {

    /** Adapter that receives the time of the cycle. */
    private final JasonCaptureAdapter adapter;

    /** Listener that does the capture. */
    private final ReasoningListener delegate;

    /** Time spent in the capture since the cycle started, in nanoseconds. */
    private long nanos;

    /**
     * Creates the listener.
     *
     * @param adapter  adapter that receives the time of each cycle
     * @param delegate listener that does the capture
     */
    MeteredReasoningListener(JasonCaptureAdapter adapter, ReasoningListener delegate) {
        this.adapter = adapter;
        this.delegate = delegate;
    }

    @Override
    public void cycleFinished(TransitionSystem ts, int cycle) {
        this.adapter.captureNanos(this.nanos);
        try {
            this.delegate.cycleFinished(ts, cycle);
        } finally {
            this.nanos = 0;
        }
    }

    @Override
    public void cycleStarting(TransitionSystem ts, int cycle) {
        long t0 = System.nanoTime();
        try {
            this.delegate.cycleStarting(ts, cycle);
        } finally {
            this.nanos += System.nanoTime() - t0;
        }
    }

    @Override
    public void phaseStarting(TransitionSystem ts, Phase phase) {
        long t0 = System.nanoTime();
        try {
            this.delegate.phaseStarting(ts, phase);
        } finally {
            this.nanos += System.nanoTime() - t0;
        }
    }

    @Override
    public void stepStarting(TransitionSystem ts, Step step) {
        long t0 = System.nanoTime();
        try {
            this.delegate.stepStarting(ts, step);
        } finally {
            this.nanos += System.nanoTime() - t0;
        }
    }

    @Override
    public void stepFinished(TransitionSystem ts, Step step) {
        long t0 = System.nanoTime();
        try {
            this.delegate.stepFinished(ts, step);
        } finally {
            this.nanos += System.nanoTime() - t0;
        }
    }

    @Override
    public void planSearchStarting(TransitionSystem ts, Event event, SearchMethod method) {
        long t0 = System.nanoTime();
        try {
            this.delegate.planSearchStarting(ts, event, method);
        } finally {
            this.nanos += System.nanoTime() - t0;
        }
    }

    @Override
    public void planEvaluated(TransitionSystem ts, Plan plan, boolean relevant, boolean applicable,
                              List<TermResult> terms) {
        long t0 = System.nanoTime();
        try {
            this.delegate.planEvaluated(ts, plan, relevant, applicable, terms);
        } finally {
            this.nanos += System.nanoTime() - t0;
        }
    }

    @Override
    public void messageSent(TransitionSystem ts, Message m) {
        long t0 = System.nanoTime();
        try {
            this.delegate.messageSent(ts, m);
        } finally {
            this.nanos += System.nanoTime() - t0;
        }
    }
}
