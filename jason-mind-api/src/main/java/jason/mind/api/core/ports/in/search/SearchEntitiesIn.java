package jason.mind.api.core.ports.in.search;

import java.util.Set;

public record SearchEntitiesIn(String agent, String q, Set<String> types, Integer from, Integer to, Integer limit) {
}
