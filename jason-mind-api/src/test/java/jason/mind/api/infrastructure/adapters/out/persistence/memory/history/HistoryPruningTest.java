package jason.mind.api.infrastructure.adapters.out.persistence.memory.history;

import jason.mind.api.core.domain.model.entities.Message;
import jason.mind.api.core.domain.model.entities.Performative;
import jason.mind.api.core.domain.model.entities.SendMode;
import jason.mind.api.core.domain.model.history.Agent;
import jason.mind.api.core.domain.model.history.AgentConfig;
import jason.mind.api.core.domain.model.timeline.Moment;
import jason.mind.api.core.domain.model.timeline.Occurrence;
import jason.mind.api.core.domain.model.timeline.ReasoningStep;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class HistoryPruningTest {

    private static Occurrence at(String agent, int cycle, long sequence) {
        return new Occurrence(Moment.of(agent, cycle, 0, ReasoningStep.PERCEIVE, 1), sequence, null);
    }

    /** Message sent by bob to alice in cycle 1, ended by alice in cycle 2 unless {@code ended} is false. */
    private static Message message(long id, boolean ended) {
        Message m = new Message(id, at("bob", 1, id * 10), 1, SendMode.ASYNCHRONOUS, null, null, "alice",
                Performative.TELL, "hi");
        if (ended) {
            m.finish(at("alice", 2, id * 10 + 1));
        }
        return m;
    }

    private static Agent bob() {
        return new Agent("bob", at("bob", 1, 1), AgentConfig.defaults());
    }

    @Test
    void pruningReportsTheMessagesThatEndedBeforeTheHorizonAndOnlyThem() {
        List<Long> pruned = new ArrayList<>();
        AgentStore store = new AgentStore(bob(), pruned::add);
        store.entities.put(5L, message(5, true));
        store.entities.put(6L, message(6, false));
        store.prune(10);
        assertEquals(List.of(5L), pruned);
        assertNull(store.entities.get(5L));
        assertNotNull(store.entities.get(6L));
    }

    @Test
    void aMessageStillInsideTheWindowIsNotReported() {
        List<Long> pruned = new ArrayList<>();
        AgentStore store = new AgentStore(bob(), pruned::add);
        store.entities.put(5L, message(5, true));
        store.prune(1);
        assertEquals(List.of(), pruned);
    }

    @Test
    void theMemoryForwardsThePrunedMessagesToItsListener() {
        HistoryMemory memory = new HistoryMemory();
        List<Long> pruned = new ArrayList<>();
        memory.onMessagePruned(pruned::add);
        AgentStore store = memory.newStore(bob());
        store.entities.put(5L, message(5, true));
        store.prune(10);
        assertEquals(List.of(5L), pruned);
    }

    @Test
    void anEndedAgentWithNothingLeftLosesItsStore() {
        HistoryMemory memory = new HistoryMemory();
        HistoryRepositoryAdapter repository = new HistoryRepositoryAdapter(memory);
        Agent bob = bob();
        bob.finish(at("bob", 3, 100));
        repository.saveAgent(bob);
        repository.prune("bob", 10);
        assertFalse(repository.loadAgent("bob").isPresent());
    }

    @Test
    void anAgentThatIsStillAliveKeepsItsEmptyStore() {
        HistoryMemory memory = new HistoryMemory();
        HistoryRepositoryAdapter repository = new HistoryRepositoryAdapter(memory);
        repository.saveAgent(bob());
        repository.prune("bob", 10);
        assertEquals("bob", repository.loadAgent("bob").orElseThrow().getName());
    }

    @Test
    void anEndedAgentThatStillHoldsAnEntityKeepsItsStore() {
        HistoryMemory memory = new HistoryMemory();
        HistoryRepositoryAdapter repository = new HistoryRepositoryAdapter(memory);
        Agent bob = bob();
        bob.finish(at("bob", 3, 100));
        repository.saveAgent(bob);
        repository.appendEntity("bob", message(6, false));
        repository.prune("bob", 10);
        assertEquals("bob", repository.loadAgent("bob").orElseThrow().getName());
    }
}
