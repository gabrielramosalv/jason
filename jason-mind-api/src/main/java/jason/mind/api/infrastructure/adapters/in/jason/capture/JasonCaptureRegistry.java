package jason.mind.api.infrastructure.adapters.in.jason.capture;

import jason.mind.api.infrastructure.metrics.MindTiming;
import jason.architecture.AgArch;
import jason.asSemantics.Agent;
import jason.infra.local.LocalAgArch;
import jason.mind.api.core.domain.model.history.AgentConfig;
import jason.mind.api.core.ports.in.capture.StartAgentIn;
import jason.mind.api.core.ports.in.capture.StartMasIn;
import jason.runtime.Settings;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Registry that attaches a capture adapter to each Jason agent.
 * <p>
 * Starts the recording of the multi-agent system on the first registration, starts the recording of each agent,
 * installs its reasoning listener and console handler, and ends the recording when the agent is removed.
 *
 * @author Gabriel Lima
 */
@RequiredArgsConstructor
public final class JasonCaptureRegistry {

    /** Logger for registration failures. */
    private static final Logger LOGGER = Logger.getLogger("Mind");

    /** Capture ports shared by every agent adapter. */
    private final CapturePorts ports;

    /** Supplier of the multi-agent system name, read once when the recording starts. */
    private final Supplier<String> masName;

    /** Directory of message entities shared by every agent adapter. */
    private final MessageDirectory directory = new MessageDirectory();

    /** Cache of source files shared by every agent adapter. */
    private final SourceFiles sourceFiles = new SourceFiles();

    /** Capture adapter of each registered agent. */
    private final Map<Agent, JasonCaptureAdapter> adapters = new ConcurrentHashMap<>();

    /** Console handler attached to the logger of each registered agent, kept to detach it when the agent is removed. */
    private final Map<Agent, AgentConsoleHandler> handlers = new ConcurrentHashMap<>();

    /**
     * Forgets a message that the history pruned, so the directory does not keep an entry for every message ever sent.
     *
     * @param entityId id of the pruned message entity
     */
    public void forgetMessage(long entityId) {
        this.directory.forget(entityId);
    }

    /** Whether the recording of the multi-agent system has already started. */
    private volatile boolean masStarted;

    /**
     * Reads the reasoning configuration of an agent.
     * <p>
     * Uses the agent settings, the cycle counts of the last {@link LocalAgArch} in the architecture chain and
     * whether each selection function keeps the default {@link Agent} implementation. Missing settings fall back to
     * {@link AgentConfig#defaults()}.
     *
     * @param ag Jason agent to inspect
     * @return the reasoning configuration of the agent
     */
    static AgentConfig config(Agent ag) {
        Settings s = ag.getTS().getSettings();
        AgentConfig d = AgentConfig.defaults();
        int sense = d.cyclesSense(), deliberate = d.cyclesDeliberate(), act = d.cyclesAct();
        AgArch arch = ag.getTS().getAgArch();
        for (AgArch a = arch; a != null; a = a.getNextAgArch()) {
            if (a instanceof LocalAgArch l) {
                sense = l.getCyclesSense();
                deliberate = l.getCyclesDeliberate();
                act = l.getCyclesAct();
            }
        }
        Class<?> c = ag.getClass();
        return new AgentConfig(s == null ? d.nrcbp() : s.nrcbp(), sense, deliberate, act,
                s == null ? d.eventsPolicy() : s.requeue() ? "requeue" : s.retrieve() ? "retrieve" : "discard",
                s == null ? d.tro() : s.isTROon(), s == null ? d.sync() : s.isSync(),
                isDefault(c, "selectEvent", Queue.class), isDefault(c, "selectOption", List.class),
                isDefault(c, "selectIntention", Queue.class), isDefault(c, "selectMessage", Queue.class),
                isDefault(c, "selectAction", Queue.class), c.getName());
    }

    /**
     * Checks whether a selection function keeps the default implementation.
     *
     * @param c      agent class to inspect
     * @param method name of the selection function
     * @param param  parameter type of the selection function
     * @return {@code true} when the method is declared by {@link Agent} or cannot be found
     */
    private static boolean isDefault(Class<?> c, String method, Class<?> param) {
        try {
            return c.getMethod(method, param).getDeclaringClass() == Agent.class;
        } catch (NoSuchMethodException e) {
            return true;
        }
    }

    /**
     * Starts capturing an agent.
     * <p>
     * When the agent cannot be started, a warning is logged and no adapter is attached.
     *
     * @param ag Jason agent to capture
     */
    public void register(Agent ag) {
        this.startMas();
        String name = ag.getTS().getAgArch().getAgName();
        try {
            this.ports.startAgent().execute(new StartAgentIn(name, config(ag), null, null));
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Mind: could not register agent " + name, e);
            return;
        }
        JasonCaptureAdapter adapter = new JasonCaptureAdapter(this.ports, this.directory, this.sourceFiles, ag, name);
        this.adapters.put(ag, adapter);
        ag.getTS().setReasoningListener(MindTiming.TIMING ? new TimedReasoningListener(adapter, name) : adapter);
        AgentConsoleHandler handler = new AgentConsoleHandler(adapter);
        this.handlers.put(ag, handler);
        ag.getTS().getLogger().addHandler(handler);
    }

    /**
     * Stops capturing an agent and ends its recording; does nothing when the agent is not registered.
     *
     * @param ag Jason agent to stop capturing
     */
    public void remove(Agent ag) {
        JasonCaptureAdapter adapter = this.adapters.remove(ag);
        if (adapter == null) {
            return;
        }
        ag.getTS().setReasoningListener(null);
        AgentConsoleHandler handler = this.handlers.remove(ag);
        if (handler != null) {
            ag.getTS().getLogger().removeHandler(handler);
        }
        adapter.end();
    }

    /**
     * Starts the recording of the multi-agent system exactly once, naming it {@code mas} when the supplied name is
     * blank.
     */
    private void startMas() {
        if (this.masStarted) {
            return;
        }
        synchronized (this) {
            if (this.masStarted) {
                return;
            }
            String name = this.masName.get();
            this.ports.startMas().execute(new StartMasIn(name == null || name.isBlank() ? "mas" : name));
            this.masStarted = true;
        }
    }
}
