package jason.mind.api.bootstrap;

import jason.architecture.MindInspectorWeb;
import jason.asSemantics.Agent;
import jason.infra.local.BaseLocalMAS;
import jason.util.Config;
import net.peelweb.PeelApp;
import net.peelweb.PeelAppBuilder;
import org.w3c.dom.Document;

import java.util.logging.Logger;

public class MindApiManager extends MindInspectorWeb {

    private static final Logger LOGGER = Logger.getLogger("Mind");

    private final MindApiModule module = new MindApiModule(jasonVersion(), MindApiManager::masName);

    private PeelApp app;


    private static String jasonVersion() {
        try {
            return Config.get().getJasonVersion();
        } catch (RuntimeException e) {
            return "unknown";
        }
    }

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
        this.app = PeelAppBuilder.run(builder -> {
            builder.port(port).host(System.getProperty("mind.host", "127.0.0.1"));
            for (Object c : module.controllers()) builder.addController(c);
        });
        this.app.start();
        LOGGER.info("Mind API v2 at http://127.0.0.1:" + port + "/api/mas");
        return "";
    }

    @Override
    public void stoptHttpServer() {
        if (this.app != null) this.app.stop();
    }

    @Override
    public void registerAg(Agent agent) {
        module.captureRegistry().register(agent);
    }

    @Override
    public void removeAg(Agent agent) {
        module.captureRegistry().remove(agent);
    }

    @Override
    public void addAgState(Agent agent, Document document, boolean b) {
    }
}
