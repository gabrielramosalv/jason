package jason.mind.api.core.application.history;

import java.util.Map;

import jason.mind.api.core.domain.model.timeline.Entity;

public final class EntityIds {

    private static final Map<String, String> PREFIX = Map.of("belief", "b", "intention", "i", "event", "e", "message", "m",
            "action", "a", "intendedMeans", "im", "plan", "p", "timer", "t");

    private EntityIds() {
    }

    public static String format(Entity<?> e) {
        return e == null ? null : PREFIX.getOrDefault(e.typeName(), "x") + e.getId();
    }

    public static String format(String type, long id) {
        return PREFIX.getOrDefault(type, "x") + id;
    }

    public static long parse(String id) {
        if (id == null || id.isEmpty()) return -1;
        int i = 0;
        while (i < id.length() && Character.isLetter(id.charAt(i))) i++;
        String digits = id.substring(i);
        if (digits.isEmpty() || !digits.chars().allMatch(Character::isDigit) || digits.length() > 18) return -1;
        if (i > 0 && !PREFIX.containsValue(id.substring(0, i))) return -1;
        return Long.parseLong(digits);
    }
}
