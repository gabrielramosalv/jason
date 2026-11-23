package jason.mind.api.infrastructure.adapters.in.web.agent;

import jason.mind.api.core.ports.in.agent.AgentStateOut;
import jason.mind.api.core.ports.in.agent.FindAgentStateIn;
import jason.mind.api.core.ports.in.agent.FindAgentStatePort;
import jason.mind.api.infrastructure.adapters.in.web.WebExchange;
import net.peelweb.context.controller.Controller;
import net.peelweb.context.controller.Mapping;
import net.peelweb.context.endpoint.Request;
import net.peelweb.context.endpoint.Response;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Controller("/api/agents")
public class AgentStateController {

    private final FindAgentStatePort findAgentState;

    public AgentStateController(FindAgentStatePort findAgentState) {
        this.findAgentState = findAgentState;
    }

    @Mapping("/{name}")
    public Response state(Request request) {
        return WebExchange.handle(() -> {
            Integer cycle = WebExchange.integer(request, "cycle");
            Set<String> include = WebExchange.set(request, "include");
            AgentStateOut out = findAgentState.execute(new FindAgentStateIn(request.getPathVariable("name"), cycle,
                    include == null ? Set.of() : include));
            Set<String> parts = new TreeSet<>();
            if (out.beliefs() != null) parts.add("beliefs");
            if (out.intentions() != null) parts.add("intentions");
            if (out.events() != null) parts.add("events");
            String etag = out.cycle() + ":" + String.join(",", parts);
            return WebExchange.withETag(request, etag, () -> body(out));
        });
    }

    private static Map<String, Object> body(AgentStateOut out) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("agent", out.agent());
        m.put("cycle", out.cycle());
        m.put("clamped", out.clamped());
        m.put("oldestCycle", out.oldestCycle());
        m.put("newestCycle", out.newestCycle());
        m.put("sequenceEnd", out.sequenceEnd());
        if (out.beliefs() != null) m.put("beliefs", out.beliefs());
        if (out.intentions() != null) {
            m.put("intentions", out.intentions());
            m.put("intendedMeans", out.intendedMeans());
        }
        if (out.events() != null) m.put("events", out.events());
        return m;
    }
}
