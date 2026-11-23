package jason.mind.api.infrastructure.adapters.in.jason.capture;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class MessageDirectory {

    private final Map<String, Long> ids = new ConcurrentHashMap<>();

    void put(String msgId, String receiver, long id) {
        if (msgId != null && receiver != null) ids.put(key(msgId, receiver), id);
    }

    Long get(String msgId, String receiver) {
        if (msgId == null || receiver == null) return null;
        return ids.get(key(msgId, receiver));
    }

    private static String key(String msgId, String receiver) {
        return msgId + '\u0000' + receiver;
    }
}
