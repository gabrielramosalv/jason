package jason.mind.api.core.application.history;

import jason.mind.api.core.domain.model.entities.Binding;
import jason.mind.api.core.domain.model.entities.IntendedMeans;
import jason.mind.api.core.domain.model.entities.Plan;
import jason.mind.api.core.domain.model.program.Trigger;
import jason.mind.api.core.domain.model.timeline.Moment;
import jason.mind.api.core.domain.model.timeline.Occurrence;
import jason.mind.api.core.domain.model.timeline.ReasoningStep;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InstantiatedLabelTest {

    private static IntendedMeans meansOf(String literal) {
        Moment m = Moment.of("bob", 1, 0, ReasoningStep.ADD_INTENDED_MEANS, 1);
        Occurrence start = new Occurrence(m, 5, null);
        Plan plan = new Plan(1, start, "p1", 1, new Trigger(Trigger.Operator.ADD, Trigger.Type.ACHIEVE, literal, List.of()),
                List.of(), List.of(), List.of(), null);
        return new IntendedMeans(2, start, plan);
    }

    @Test
    void theVariablesBoundWhenTheMeansWasCreatedAreReplacedInTheLabel() {
        IntendedMeans im = meansOf("winner(1,_155,_156)");
        Moment born = im.getStart().moment();
        im.addBinding(new Binding("_155", "[offer(103.57,p3)]", born, 6));
        assertEquals("+!winner(1,[offer(103.57,p3)],_156)", InstantiatedLabel.of(im));
    }

    @Test
    void aBindingMadeInALaterStepDoesNotChangeTheLabel() {
        IntendedMeans im = meansOf("go(X)");
        im.addBinding(new Binding("X", "home", Moment.of("bob", 2, 0, ReasoningStep.EXECUTE_INTENTION, 1), 9));
        assertEquals("+!go(X)", InstantiatedLabel.of(im));
    }

    @Test
    void aVariableNameInsideAnotherNameIsLeftAlone() {
        IntendedMeans im = meansOf("go(X,XY)");
        im.addBinding(new Binding("X", "a", im.getStart().moment(), 6));
        assertEquals("+!go(a,XY)", InstantiatedLabel.of(im));
    }
}
