package jason.mind.api.infrastructure.adapters.in.web;

import jason.mind.api.infrastructure.metrics.MindTiming;
import lombok.RequiredArgsConstructor;
import net.peelweb.json.JsonConverter;
import net.peelweb.json.JsonConverterManager;

/**
 * JSON converter that times the serialization of each response, used only when {@link MindTiming#TIMING} is on.
 *
 * @author Gabriel Lima
 */
@RequiredArgsConstructor
public final class TimedJsonConverter implements JsonConverter {

    /** Converter that does the conversion. */
    private final JsonConverter delegate;

    /**
     * Replaces the converter of the web library with a timed one when {@link MindTiming#TIMING} is on.
     */
    public static void install() {
        if (!MindTiming.TIMING) {
            return;
        }
        JsonConverterManager manager = JsonConverterManager.getInstance();
        if (!(manager.getConverter() instanceof TimedJsonConverter)) {
            manager.setConverter(new TimedJsonConverter(manager.getConverter()));
        }
    }

    @Override
    public String toJson(Object value) {
        long t0 = System.nanoTime();
        try {
            return this.delegate.toJson(value);
        } finally {
            MindTiming.addSerialization(System.nanoTime() - t0);
        }
    }

    @Override
    public <T> T fromJson(String json, Class<T> type) {
        return this.delegate.fromJson(json, type);
    }
}
