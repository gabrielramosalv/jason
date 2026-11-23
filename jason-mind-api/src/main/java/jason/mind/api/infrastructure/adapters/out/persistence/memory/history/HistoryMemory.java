package jason.mind.api.infrastructure.adapters.out.persistence.memory.history;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class HistoryMemory {

    final Map<String, AgentStore> stores = new ConcurrentHashMap<>();

    AgentStore store(String agent) {
        return stores.get(agent);
    }
}
