package jason.mind.api.core.application.history;

import jason.mind.api.core.domain.model.entities.Binding;
import jason.mind.api.core.domain.model.entities.IntendedMeans;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds the label of an intended means with the variables bound when it was created, as Jason shows it.
 * The label of the entity stays free of the unifier; the instantiated text is only for the intention stack.
 *
 * @author Gabriel Lima
 */
public final class InstantiatedLabel {

    private InstantiatedLabel() {
    }

    /**
     * Replaces in the label of an intended means the variables bound in the step that created it.
     *
     * @param im intended means whose label is wanted
     * @return the label with the creation bindings applied, or the plain label when there are none
     */
    public static String of(IntendedMeans im) {
        String label = im.label();
        List<Binding> bindings = im.getBindings();
        for (Binding b : bindings) {
            if (!b.moment().equals(im.getStart().moment())) {
                continue;
            }
            Pattern p = Pattern.compile("(?<![A-Za-z0-9_])" + Pattern.quote(b.name()) + "(?![A-Za-z0-9_])");
            label = p.matcher(label).replaceAll(Matcher.quoteReplacement(b.value()));
        }
        return label;
    }
}
