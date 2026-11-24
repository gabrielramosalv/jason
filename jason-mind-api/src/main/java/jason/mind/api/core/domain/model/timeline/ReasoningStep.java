package jason.mind.api.core.domain.model.timeline;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Step of the reasoning cycle, in execution order.
 * Each step belongs to a reasoning phase.
 *
 * @author Gabriel Lima
 */
@Getter
@RequiredArgsConstructor
public enum ReasoningStep {
    /** Perceives the environment. */
    PERCEIVE(ReasoningPhase.SENSE),
    /** Processes received messages. */
    PROCESS_MESSAGES(ReasoningPhase.SENSE),
    /** Runs the tasks of the start of the cycle. */
    CYCLE_START_TASKS(ReasoningPhase.DELIBERATE),
    /** Selects an event. */
    SELECT_EVENT(ReasoningPhase.DELIBERATE),
    /** Finds an option for the selected event. */
    FIND_OPTION(ReasoningPhase.DELIBERATE),
    /** Adds the intended means of the chosen option. */
    ADD_INTENDED_MEANS(ReasoningPhase.DELIBERATE),
    /** Processes the feedback of actions. */
    PROCESS_ACTIONS(ReasoningPhase.ACT),
    /** Selects an intention. */
    SELECT_INTENTION(ReasoningPhase.ACT),
    /** Executes the selected intention. */
    EXECUTE_INTENTION(ReasoningPhase.ACT),
    /** Clears finished intentions. */
    CLEAR_INTENTION(ReasoningPhase.ACT),
    /** Loads the initial program. */
    INITIAL_LOAD(ReasoningPhase.EXTERNAL),
    /** Happens outside the reasoning cycle. */
    OUT_OF_CYCLE(ReasoningPhase.EXTERNAL);

    /** Phase the step belongs to. */
    private final ReasoningPhase phase;
}
