package jason.mind.api.bootstrap;

import jason.mind.api.infrastructure.adapters.in.web.TimedJsonConverter;
import jason.mind.api.infrastructure.metrics.MindTiming;
import jason.architecture.MindInspectorWeb;
import jason.asSemantics.Agent;
import jason.infra.local.BaseLocalMAS;
import jason.util.Config;
import net.peelweb.PeelApp;
import net.peelweb.PeelAppBuilder;
import org.w3c.dom.Document;

import java.util.logging.Logger;

/**
 * Mind inspector that serves the Mind API over HTTP.
 * Starts and stops the HTTP server with the API controllers and forwards agent registration to the capture registry.
 *
 * @author Gabriel Lima
 */
public class MindApiManager extends MindInspectorWeb {

    /** Logger for the server startup. */
    private static final Logger LOGGER = Logger.getLogger("Mind");

    /** Wiring of the Mind API components. */
    private final MindApiModule module = MindApiModule.create(jasonVersion(), MindApiManager::masName);

    /** Running HTTP application, or {@code null} before the server starts. */
    private PeelApp app;

    /**
     * Reads the version of Jason from its configuration.
     *
     * @return version of Jason, or {@code "unknown"} when it cannot be read
     */
    private static String jasonVersion() {
        try {
            String v = Config.get().getJasonVersion();
            return v == null || v.startsWith("undefined") ? "unknown" : v;
        } catch (RuntimeException e) {
            return "unknown";
        }
    }

    /**
     * Reads the name of the running multi-agent system from the local runner.
     *
     * @return name of the multi-agent system, or {@code null} when there is no runner or project
     */
    private static String masName() {
        try {
            BaseLocalMAS runner = BaseLocalMAS.getRunner();
            return runner == null || runner.getProject() == null ? null : runner.getProject().getSocName();
        } catch (RuntimeException e) {
            return null;
        }
    }

    @Override
    public String startHttpServer() {
        int port = super.httpServerPort;
        TimedJsonConverter.install();
        this.app = PeelAppBuilder.run(builder -> {
            builder.port(port).host(System.getProperty("mind.host", "127.0.0.1"));
            if (MindTiming.TIMING) {
                builder.requestObserver((method, path, status, startNanos, endNanos) ->
                        MindTiming.addRequest(endNanos - startNanos));
            }
            for (Object c : this.module.controllers()) {
                builder.addController(c);
            }
        });
        this.app.start();
        LOGGER.info("Mind API at http://127.0.0.1:" + port + "/api/mas");
        return "";
    }

    @Override
    public void stoptHttpServer() {
        if (this.app != null) {
            this.app.stop();
        }
    }

    @Override
    public void registerAg(Agent agent) {
        this.module.captureRegistry().register(agent);
    }

    @Override
    public void removeAg(Agent agent) {
        this.module.captureRegistry().remove(agent);
    }

    @Override
    public void addAgState(Agent agent, Document document, boolean b) {
    }
}
