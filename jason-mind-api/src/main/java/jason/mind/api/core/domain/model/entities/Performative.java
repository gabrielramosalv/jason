package jason.mind.api.core.domain.model.entities;

import java.util.Objects;
import java.util.Set;

public record Performative(String name) {

    public static final Performative TELL = new Performative("tell");
    public static final Performative UNTELL = new Performative("untell");
    public static final Performative ACHIEVE = new Performative("achieve");
    public static final Performative UNACHIEVE = new Performative("unachieve");
    public static final Performative ASK_ONE = new Performative("askOne");
    public static final Performative ASK_ALL = new Performative("askAll");
    public static final Performative TELL_HOW = new Performative("tellHow");
    public static final Performative UNTELL_HOW = new Performative("untellHow");
    public static final Performative ASK_HOW = new Performative("askHow");
    public static final Performative SIGNAL = new Performative("signal");

    private static final Set<String> KNOWN = Set.of("tell", "untell", "achieve", "unachieve", "askOne", "askAll",
            "tellHow", "untellHow", "askHow", "signal");

    public Performative {
        Objects.requireNonNull(name, "name");
    }

    public boolean isKnown() {
        return KNOWN.contains(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
