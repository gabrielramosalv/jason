package jason.mind.api.core.application.history;

import jason.mind.api.core.domain.model.timeline.Entity;

import java.util.Map;

/**
 * Utility that formats and parses the public ids of entities.
 * A public id is a type prefix followed by the numeric id, such as {@code b12} for a belief;
 * unknown types use the prefix {@code x}.
 *
 * @author Gabriel Lima
 */
public final class EntityIds {

    /** Id prefix of each entity type name. */
    private static final Map<String, String> PREFIX = Map.of("belief", "b", "intention", "i", "event", "e", "message",
            "m", "action", "a", "intendedMeans", "im", "plan", "p", "timer", "t");

    /**
     * Prevents instantiation of this utility class.
     */
    private EntityIds() {
    }

    /**
     * Formats the public id of an entity.
     *
     * @param e entity to identify, possibly {@code null}
     * @return the public id, or {@code null} when the entity is {@code null}
     */
    public static String format(Entity e) {
        return e == null ? null : PREFIX.getOrDefault(e.typeName(), "x") + e.getId();
    }

    /**
     * Formats the public id of an entity from its type name and numeric id.
     *
     * @param type entity type name
     * @param id   numeric id of the entity
     * @return the public id
     */
    public static String format(String type, long id) {
        return PREFIX.getOrDefault(type, "x") + id;
    }

    /**
     * Parses a public id into its numeric id. The prefix is optional, but when present it must be a known one.
     *
     * @param id public id to parse, possibly {@code null}
     * @return the numeric id, or {@code -1} when the id is empty, malformed or has an unknown prefix
     */
    public static long parse(String id) {
        if (id == null || id.isEmpty()) {
            return -1;
        }
        int i = 0;
        while (i < id.length() && Character.isLetter(id.charAt(i))) {
            i++;
        }
        String digits = id.substring(i);
        if (digits.isEmpty() || !digits.chars().allMatch(Character::isDigit) || digits.length() > 18) {
            return -1;
        }
        if (i > 0 && !PREFIX.containsValue(id.substring(0, i))) {
            return -1;
        }
        return Long.parseLong(digits);
    }
}
