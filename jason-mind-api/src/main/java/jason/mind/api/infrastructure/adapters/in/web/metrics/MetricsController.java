package jason.mind.api.infrastructure.adapters.in.web.metrics;

import jason.mind.api.core.ports.in.metrics.FindMetricsPort;
import jason.mind.api.infrastructure.adapters.in.web.WebExchange;
import lombok.RequiredArgsConstructor;
import net.peelweb.context.controller.Controller;
import net.peelweb.context.controller.Mapping;
import net.peelweb.context.endpoint.Response;
import net.peelweb.context.endpoint.Responses;

/**
 * Controller of the light monitoring. It has no ETag: the values change at every poll.
 *
 * @author Gabriel Lima
 */
@Controller("/api/metrics")
@RequiredArgsConstructor
public class MetricsController {

    /** Input port that finds the indicators. */
    private final FindMetricsPort findMetrics;

    /**
     * Returns the heap of the JVM and the indicators of each agent.
     *
     * @return response with the indicators
     */
    @Mapping
    public Response find() {
        return WebExchange.handle("metrics", () -> Responses.ok(this.findMetrics.execute()));
    }
}
