package jason.mind.api.core.domain.model.entities;

import java.util.Objects;
import java.util.Set;

/**
 * Performative of a message.
 * Offers constants for the standard performatives and tells whether a name is one of them.
 *
 * @param name name of the performative
 * @author Gabriel Lima
 */
public record Performative(String name) {

    /** Standard performative tell. */
    public static final Performative TELL = new Performative("tell");

    /** Standard performative untell. */
    public static final Performative UNTELL = new Performative("untell");

    /** Standard performative achieve. */
    public static final Performative ACHIEVE = new Performative("achieve");

    /** Standard performative unachieve. */
    public static final Performative UNACHIEVE = new Performative("unachieve");

    /** Standard performative askOne. */
    public static final Performative ASK_ONE = new Performative("askOne");

    /** Standard performative askAll. */
    public static final Performative ASK_ALL = new Performative("askAll");

    /** Standard performative tellHow. */
    public static final Performative TELL_HOW = new Performative("tellHow");

    /** Standard performative untellHow. */
    public static final Performative UNTELL_HOW = new Performative("untellHow");

    /** Standard performative askHow. */
    public static final Performative ASK_HOW = new Performative("askHow");

    /** Standard performative signal. */
    public static final Performative SIGNAL = new Performative("signal");

    /** Names of the standard performatives. */
    private static final Set<String> KNOWN = Set.of("tell", "untell", "achieve", "unachieve", "askOne", "askAll",
            "tellHow", "untellHow", "askHow", "signal");

    /**
     * Validates the performative.
     *
     * @throws NullPointerException if the name is null
     */
    public Performative {
        Objects.requireNonNull(name, "name");
    }

    /**
     * Tells whether the performative is one of the standard ones.
     *
     * @return true if the name is a standard performative
     */
    public boolean isKnown() {
        return KNOWN.contains(this.name);
    }

    @Override
    public String toString() {
        return this.name;
    }
}
