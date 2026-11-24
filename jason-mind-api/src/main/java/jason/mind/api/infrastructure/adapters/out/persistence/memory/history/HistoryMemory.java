package jason.mind.api.infrastructure.adapters.out.persistence.memory.history;

import java.util.Map;
import jason.mind.api.core.domain.model.history.Agent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongConsumer;

/**
 * In-memory holder of the history stores of all agents.
 * Shared by the history repository adapters so writes and queries see the same data.
 *
 * @author Gabriel Lima
 */
public final class HistoryMemory {

    /** History store of each agent, by agent name. */
    final Map<String, AgentStore> stores = new ConcurrentHashMap<>();

    /** Called with the id of every message removed by pruning; does nothing until a listener is set. */
    private volatile LongConsumer messagePruned = id -> {
    };

    /**
     * Sets the listener of the messages removed by pruning.
     *
     * @param listener receives the id of each pruned message
     */
    public void onMessagePruned(LongConsumer listener) {
        this.messagePruned = listener;
    }

    /**
     * Creates the store of an agent that reports its pruned messages to the current listener.
     *
     * @param agent agent whose history the store holds
     * @return empty store of the agent
     */
    AgentStore newStore(Agent agent) {
        return new AgentStore(agent, id -> this.messagePruned.accept(id));
    }

    /**
     * Finds the history store of an agent.
     *
     * @param agent name of the agent
     * @return store of the agent, or {@code null} when the agent has no store
     */
    AgentStore findStore(String agent) {
        return this.stores.get(agent);
    }
}
