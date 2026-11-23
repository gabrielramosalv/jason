package jason.mind.api.infrastructure.adapters.in.jason.capture;

import jason.architecture.AgArch;
import jason.asSemantics.Agent;
import jason.infra.local.LocalAgArch;
import jason.mind.api.core.domain.model.history.AgentConfig;
import jason.mind.api.core.ports.in.capture.StartAgentIn;
import jason.mind.api.core.ports.in.capture.StartMasIn;
import jason.runtime.Settings;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class JasonCaptureRegistry {

    private static final Logger LOGGER = Logger.getLogger("Mind");

    private final CapturePorts ports;
    private final Supplier<String> masName;
    private final MessageDirectory directory = new MessageDirectory();
    private final SourceFiles sourceFiles = new SourceFiles();
    private final Map<Agent, JasonCaptureAdapter> adapters = new ConcurrentHashMap<>();
    private volatile boolean masStarted;

    public JasonCaptureRegistry(CapturePorts ports, Supplier<String> masName) {
        this.ports = ports;
        this.masName = masName;
    }

    public void register(Agent ag) {
        startMas();
        String name = ag.getTS().getAgArch().getAgName();
        try {
            ports.startAgent().execute(new StartAgentIn(name, config(ag), null, null));
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Mind: could not register agent " + name, e);
            return;
        }
        JasonCaptureAdapter adapter = new JasonCaptureAdapter(ports, directory, sourceFiles, ag, name);
        adapters.put(ag, adapter);
        ag.getTS().setReasoningListener(adapter);
        ag.getTS().getLogger().addHandler(new AgentConsoleHandler(adapter));
    }

    public void remove(Agent ag) {
        JasonCaptureAdapter adapter = adapters.remove(ag);
        if (adapter == null) return;
        ag.getTS().setReasoningListener(null);
        adapter.end();
    }

    private void startMas() {
        if (masStarted) return;
        synchronized (this) {
            if (masStarted) return;
            String name = masName.get();
            ports.startMas().execute(new StartMasIn(name == null || name.isBlank() ? "mas" : name));
            masStarted = true;
        }
    }

    static AgentConfig config(Agent ag) {
        Settings s = ag.getTS().getSettings();
        AgentConfig d = AgentConfig.defaults();
        int sense = d.cyclesSense(), deliberate = d.cyclesDeliberate(), act = d.cyclesAct();
        AgArch arch = ag.getTS().getAgArch();
        for (AgArch a = arch; a != null; a = a.getNextAgArch())
            if (a instanceof LocalAgArch l) {
                sense = l.getCyclesSense();
                deliberate = l.getCyclesDeliberate();
                act = l.getCyclesAct();
            }
        Class<?> c = ag.getClass();
        return new AgentConfig(s == null ? d.nrcbp() : s.nrcbp(), sense, deliberate, act,
                s == null ? d.eventsPolicy() : s.requeue() ? "requeue" : s.retrieve() ? "retrieve" : "discard",
                s == null ? d.tro() : s.isTROon(), s == null ? d.sync() : s.isSync(),
                isDefault(c, "selectEvent", Queue.class), isDefault(c, "selectOption", List.class),
                isDefault(c, "selectIntention", Queue.class), isDefault(c, "selectMessage", Queue.class),
                isDefault(c, "selectAction", Queue.class), c.getName());
    }

    private static boolean isDefault(Class<?> c, String method, Class<?> param) {
        try {
            return c.getMethod(method, param).getDeclaringClass() == Agent.class;
        } catch (NoSuchMethodException e) {
            return true;
        }
    }
}
