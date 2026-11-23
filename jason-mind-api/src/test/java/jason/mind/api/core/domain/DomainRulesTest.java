package jason.mind.api.core.domain;

import jason.mind.api.core.domain.model.entities.Action;
import jason.mind.api.core.domain.model.entities.IntendedMeans;
import jason.mind.api.core.domain.model.entities.Intention;
import jason.mind.api.core.domain.model.program.SourceFile;
import jason.mind.api.core.domain.model.program.Trigger;
import jason.mind.api.core.domain.model.settings.Settings;
import jason.mind.api.core.domain.model.timeline.Moment;
import jason.mind.api.core.domain.model.timeline.Occurrence;
import jason.mind.api.core.domain.model.timeline.ReasoningStep;
import jason.mind.api.core.domain.model.timeline.Timeline;
import jason.mind.api.core.domain.service.reasoning.CandidatesDomainService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainRulesTest {

    private static Occurrence at(int cycle, ReasoningStep step, long sequence) {
        return new Occurrence(Moment.of("bob", cycle, 0, step, 1), sequence, null);
    }

    @Test
    void momentOrdersByCycleIterationStepAndOrdinal() {
        Moment a = Moment.of("bob", 3, 0, ReasoningStep.SELECT_EVENT, 1);
        Moment b = Moment.of("bob", 3, 0, ReasoningStep.EXECUTE_INTENTION, 1);
        Moment c = Moment.of("bob", 3, 1, ReasoningStep.PERCEIVE, 1);
        Moment d = Moment.of("bob", 3, 1, ReasoningStep.PERCEIVE, 2);
        Moment e = Moment.of("bob", 4, 0, ReasoningStep.PERCEIVE, 1);
        assertTrue(a.isBefore(b));
        assertTrue(b.isBefore(c));
        assertTrue(c.isBefore(d));
        assertTrue(d.isBefore(e));
    }

    @Test
    void momentsOfDifferentAgentsHaveNoOrder() {
        Moment a = Moment.of("bob", 1, 0, ReasoningStep.PERCEIVE, 1);
        Moment b = Moment.of("alice", 1, 0, ReasoningStep.PERCEIVE, 1);
        assertThrows(IllegalArgumentException.class, () -> a.compareTo(b));
    }

    @Test
    void momentRejectsNegativeIterationAndZeroOrdinal() {
        assertThrows(IllegalArgumentException.class, () -> Moment.of("bob", 1, -1, ReasoningStep.PERCEIVE, 1));
        assertThrows(IllegalArgumentException.class, () -> Moment.of("bob", 1, 0, ReasoningStep.PERCEIVE, 0));
    }

    @Test
    void timelineKeepsOnlyChangesAndAnswersByCycle() {
        Timeline<String> t = new Timeline<>();
        assertTrue(t.put(at(1, ReasoningStep.PERCEIVE, 1), "a"));
        assertFalse(t.put(at(2, ReasoningStep.PERCEIVE, 2), "a"));
        assertTrue(t.put(at(4, ReasoningStep.PERCEIVE, 3), "b"));
        assertEquals(2, t.entries().size());
        assertNull(t.at(0));
        assertEquals("a", t.at(3));
        assertEquals("b", t.at(4));
        assertEquals("a", t.before(3));
    }

    @Test
    void timelineRefusesSequenceOutOfOrder() {
        Timeline<String> t = new Timeline<>();
        t.put(at(1, ReasoningStep.PERCEIVE, 5), "a");
        assertThrows(IllegalArgumentException.class, () -> t.put(at(1, ReasoningStep.PERCEIVE, 4), "b"));
    }

    @Test
    void pruningKeepsTheValueInForceAtTheHorizon() {
        Timeline<String> t = new Timeline<>();
        t.put(at(1, ReasoningStep.PERCEIVE, 1), "a");
        t.put(at(2, ReasoningStep.PERCEIVE, 2), "b");
        t.put(at(5, ReasoningStep.PERCEIVE, 3), "c");
        t.pruneBefore(4);
        assertEquals(2, t.entries().size());
        assertEquals("b", t.at(4));
        assertEquals("c", t.at(5));
    }

    @Test
    void retentionAcceptsOnlyTheDocumentedRange() {
        assertEquals(1, new Settings(1).retentionCycles());
        assertEquals(10_000_000, new Settings(10_000_000).retentionCycles());
        assertThrows(IllegalArgumentException.class, () -> new Settings(0));
        assertThrows(IllegalArgumentException.class, () -> new Settings(10_000_001));
    }

    @Test
    void intentionStateFollowsThePriorityOfTheModel() {
        Action action = null;
        assertEquals(Intention.State.SUSPENDED, Intention.stateOf(Intention.Place.RUNNING, true, action));
        assertEquals(Intention.State.SELECTED, Intention.stateOf(Intention.Place.SELECTED_INTENTION, false, null));
        assertEquals(Intention.State.PENDING, Intention.stateOf(Intention.Place.PENDING_INTENTIONS, false, null));
        assertEquals(Intention.State.QUEUED, Intention.stateOf(Intention.Place.RUNNING, false, null));
    }

    @Test
    void candidatePlansUseTheSameKeyAsTheJasonPlanLibrary() {
        Trigger event = new Trigger(Trigger.Operator.ADD, Trigger.Type.ACHIEVE, "go(home,fast)", List.of());
        assertTrue(CandidatesDomainService.isCandidate(
                new Trigger(Trigger.Operator.ADD, Trigger.Type.ACHIEVE, "go(X,Y)", List.of()), event));
        assertFalse(CandidatesDomainService.isCandidate(
                new Trigger(Trigger.Operator.ADD, Trigger.Type.ACHIEVE, "go(X)", List.of()), event));
        assertFalse(CandidatesDomainService.isCandidate(
                new Trigger(Trigger.Operator.DELETE, Trigger.Type.ACHIEVE, "go(X,Y)", List.of()), event));
        assertFalse(CandidatesDomainService.isCandidate(
                new Trigger(Trigger.Operator.ADD, Trigger.Type.BELIEF, "go(X,Y)", List.of()), event));
        assertTrue(CandidatesDomainService.isCandidate(
                new Trigger(Trigger.Operator.ADD, Trigger.Type.ACHIEVE, "G", List.of()), event));
    }

    @Test
    void sourceFileNameDropsDirectoryAndUrlScheme() {
        assertEquals("a.asl", new SourceFile("file:a.asl", "").fileName());
        assertEquals("a.asl", new SourceFile("/home/x/a.asl", "").fileName());
        assertEquals("kqmlPlans.asl", new SourceFile("jar:file:/j/jason.jar!/asl/kqmlPlans.asl", "").fileName());
        assertEquals("a.asl", new SourceFile("C:\\x\\a.asl", "").fileName());
    }

    @Test
    void pruningDropsDeadLevelsFromALongIntention() {
        Intention i = new Intention(1, at(1, ReasoningStep.ADD_INTENDED_MEANS, 1), Intention.Place.RUNNING);
        IntendedMeans old = new IntendedMeans(2, at(1, ReasoningStep.ADD_INTENDED_MEANS, 2), null);
        IntendedMeans recent = new IntendedMeans(3, at(5, ReasoningStep.ADD_INTENDED_MEANS, 3), null);
        IntendedMeans alive = new IntendedMeans(4, at(9, ReasoningStep.ADD_INTENDED_MEANS, 4), null);
        i.push(old);
        i.push(recent);
        i.push(alive);
        old.finish(at(3, ReasoningStep.CLEAR_INTENTION, 5));
        recent.finish(at(8, ReasoningStep.CLEAR_INTENTION, 6));
        i.pruneStack(6);
        assertEquals(List.of(recent, alive), i.getStack());
    }
}
