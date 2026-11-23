package jason.mind.api.infrastructure.adapters.in.jason.capture;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

final class AgentConsoleHandler extends Handler {

    private final JasonCaptureAdapter adapter;
    private final SimpleFormatter formatter = new SimpleFormatter();

    AgentConsoleHandler(JasonCaptureAdapter adapter) {
        this.adapter = adapter;
        setLevel(Level.INFO);
    }

    @Override
    public void publish(LogRecord record) {
        if (record == null || !isLoggable(record)) return;
        adapter.consoleLine(formatter.formatMessage(record));
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }
}
