package jason.mind.api.core.ports.in.plan;

import java.util.List;

/**
 * Output with the plans of an agent.
 *
 * @param agent   name of the agent
 * @param version version of the plan library
 * @param plans   plans of the library
 * @author Gabriel Lima
 */
public record PlansOut(String agent, long version, List<PlanOut> plans) {

    /**
     * Plan.
     *
     * @param id          identifier of the plan
     * @param label       label of the plan
     * @param trigger     triggering event
     * @param context     terms of the context
     * @param source      location in the source code, or null
     * @param annotations annotations of the label
     * @param orderKey    key that orders the plan in the library
     * @param alive       cycles in which the plan existed
     * @param body        deeds of the body
     */
    public record PlanOut(String id, String label, TriggerOut trigger, List<String> context, SourceOut source,
                          List<String> annotations, long orderKey, Alive alive, List<DeedOut> body) {

    }

    /**
     * Triggering event of a plan.
     *
     * @param operator operator
     * @param type     type
     * @param literal  literal
     */
    public record TriggerOut(String operator, String type, String literal) {

    }

    /**
     * Location of a plan in the source code.
     *
     * @param path path of the file
     * @param line line in the file, or null
     */
    public record SourceOut(String path, Integer line) {

    }

    /**
     * Cycles in which a plan existed.
     *
     * @param fromCycle cycle in which the plan was added
     * @param toCycle   cycle in which the plan was removed, or null while present
     */
    public record Alive(int fromCycle, Integer toCycle) {

    }

    /**
     * Deed of a plan body.
     *
     * @param type    type of the deed
     * @param content text of the deed
     * @param line    source line, or null
     * @param parent  index of the parent deed
     */
    public record DeedOut(String type, String content, Integer line, int parent) {

    }
}
