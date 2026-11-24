package jason.mind.api.infrastructure.adapters.in.jason.capture;

import jason.mind.api.infrastructure.metrics.MindTiming;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

/**
 * Logging handler that forwards the console output of one agent to its capture adapter.
 * <p>
 * Formats each loggable record at level {@link Level#INFO} or above and hands the resulting text to
 * {@link JasonCaptureAdapter#recordConsoleLine(String)}.
 *
 * @author Gabriel Lima
 */
final class AgentConsoleHandler extends Handler {

    /** Capture adapter that receives the formatted console lines. */
    private final JasonCaptureAdapter adapter;

    /** Formatter that turns a log record into its message text. */
    private final SimpleFormatter formatter = new SimpleFormatter();

    /**
     * Creates the handler with the capture adapter that receives the console lines and sets its level to
     * {@link Level#INFO}.
     *
     * @param adapter {@link #adapter}
     */
    AgentConsoleHandler(JasonCaptureAdapter adapter) {
        this.adapter = adapter;
        super.setLevel(Level.INFO);
    }

    @Override
    public void publish(LogRecord record) {
        // The end is tested first so that an ended agent does not pay for formatting lines nobody will record.
        if (record == null || this.adapter.isEnded() || !super.isLoggable(record)) {
            return;
        }
        if (!MindTiming.TIMING) {
            this.adapter.recordConsoleLine(this.formatter.formatMessage(record));
            return;
        }
        long t0 = System.nanoTime();
        try {
            this.adapter.recordConsoleLine(this.formatter.formatMessage(record));
        } finally {
            MindTiming.addCapture(this.adapter.agentName(), System.nanoTime() - t0);
        }
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }
}
