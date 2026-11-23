package jason.asSemantics;

import java.util.List;

import jason.asSyntax.Plan;

public interface ReasoningListener {

    enum Step {
        PERCEIVE,
        PROCESS_MESSAGES,
        CYCLE_START_TASKS,
        SELECT_EVENT,
        FIND_OPTION,
        ADD_INTENDED_MEANS,
        PROCESS_ACTIONS,
        SELECT_INTENTION,
        EXECUTE_INTENTION,
        CLEAR_INTENTION
    }

    enum Phase { SENSE, DELIBERATE, ACT }

    enum SearchMethod {

        FIRST_FOUND,

        RELEVANT_AND_APPLICABLE
    }

    final class TermResult {
        public final int index;
        public final int attempts;
        public final int successes;

        public final List<String[]> bindings;

        public TermResult(int index, int attempts, int successes, List<String[]> bindings) {
            this.index = index;
            this.attempts = attempts;
            this.successes = successes;
            this.bindings = bindings;
        }
    }

    default void cycleStarting(TransitionSystem ts, int cycle) {}

    default void cycleFinished(TransitionSystem ts, int cycle) {}

    default void phaseStarting(TransitionSystem ts, Phase phase) {}

    default void stepStarting(TransitionSystem ts, Step step) {}

    default void stepFinished(TransitionSystem ts, Step step) {}

    default void planSearchStarting(TransitionSystem ts, Event event, SearchMethod method) {}

    default void planEvaluated(TransitionSystem ts, Plan plan, boolean relevant, boolean applicable, List<TermResult> terms) {}

    default void messageSent(TransitionSystem ts, Message m) {}
}
