package jason.mind.api.infrastructure.adapters.in.web.agent;

import jason.mind.api.core.ports.in.agent.AgentStateOut;
import jason.mind.api.core.ports.in.agent.FindAgentStateIn;
import jason.mind.api.core.ports.in.agent.FindAgentStatePort;
import jason.mind.api.infrastructure.adapters.in.web.WebExchange;
import lombok.RequiredArgsConstructor;
import net.peelweb.context.controller.Controller;
import net.peelweb.context.controller.Mapping;
import net.peelweb.context.endpoint.Request;
import net.peelweb.context.endpoint.Response;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Controller that exposes the mental state of an agent at a given cycle.
 * Answers with the requested parts of the state and an ETag built from the cycle, the cycle range, the sequence and the included parts.
 *
 * @author Gabriel Lima
 */
@Controller("/api/agents")
@RequiredArgsConstructor
public class AgentStateController {

    /** Input port that finds the state of an agent at a cycle. */
    private final FindAgentStatePort findAgentState;

    /**
     * Builds the response body from the agent state, leaving out the parts that were not requested.
     *
     * @param out agent state found for the request
     * @return ordered map with the fields of the response body
     */
    private static Map<String, Object> body(AgentStateOut out) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("agent", out.agent());
        m.put("cycle", out.cycle());
        m.put("clamped", out.clamped());
        m.put("oldestCycle", out.oldestCycle());
        m.put("newestCycle", out.newestCycle());
        m.put("sequenceEnd", out.sequenceEnd());
        if (out.beliefs() != null) {
            m.put("beliefs", out.beliefs());
        }
        if (out.intentions() != null) {
            m.put("intentions", out.intentions());
            m.put("intendedMeans", out.intendedMeans());
        }
        if (out.events() != null) {
            m.put("events", out.events());
        }
        return m;
    }

    /**
     * Returns the state of the agent named in the path, optionally at the cycle given by the {@code cycle}
     * parameter and limited to the parts listed in the {@code include} parameter.
     *
     * @param request HTTP request carrying the agent name, the cycle and the parts to include
     * @return response with the agent state, or not modified when the ETag still matches
     */
    @Mapping("/{name}")
    public Response getState(Request request) {
        return WebExchange.handle("agentState", () -> {
            Integer cycle = WebExchange.integer(request, "cycle");
            Set<String> include = WebExchange.set(request, "include");
            AgentStateOut out = this.findAgentState.execute(
                    new FindAgentStateIn(request.getPathVariable("name"), cycle, include == null ? Set.of() : include));
            Set<String> parts = new TreeSet<>();
            if (out.beliefs() != null) {
                parts.add("beliefs");
            }
            if (out.intentions() != null) {
                parts.add("intentions");
            }
            if (out.events() != null) {
                parts.add("events");
            }
            String etag = out.cycle() + ":" + out.oldestCycle() + "." + out.newestCycle() + "." + out.sequenceEnd() + ":"
                    + out.clamped() + ":" + String.join(",", parts);
            return WebExchange.withETag(request, etag, () -> body(out));
        });
    }
}
