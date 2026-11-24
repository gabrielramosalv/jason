package jason.mind.api.core.domain.model.program;

import java.util.Objects;

/**
 * Source of a belief or plan, read from a {@code source} annotation.
 * Holds the source term and its category.
 *
 * @param term     inner term of the source annotation
 * @param category category of the source
 * @author Gabriel Lima
 */
public record Source(String term, Category category) {

    /**
     * Validates the source.
     *
     * @throws NullPointerException if the term or the category is null
     */
    public Source {
        Objects.requireNonNull(term, "term");
        Objects.requireNonNull(category, "category");
    }

    /**
     * Builds a source from an annotation.
     *
     * @param annotation annotation to read
     * @return source of the annotation, or null when it is not a source annotation
     */
    public static Source of(Annotation annotation) {
        if (!annotation.isSource()) {
            return null;
        }
        String t = annotation.term();
        String inner = t.startsWith("source(") && t.endsWith(")") ? t.substring(7, t.length() - 1) : t;
        return new Source(inner, categoryOf(inner));
    }

    /**
     * Classifies a source term.
     *
     * @param term inner term of the source annotation
     * @return {@code SELF}, {@code PERCEPT}, {@code AGENT} for an atom, or {@code OTHER}
     */
    public static Category categoryOf(String term) {
        if ("self".equals(term)) {
            return Category.SELF;
        }
        if ("percept".equals(term)) {
            return Category.PERCEPT;
        }
        if (!term.isEmpty() && Character.isLowerCase(term.charAt(0)) && term.chars().allMatch(
                c -> Character.isLetterOrDigit(c) || c == '_')) {
            return Category.AGENT;
        }
        return Category.OTHER;
    }

    /**
     * Converts the source back into an annotation.
     *
     * @return {@code source} annotation wrapping the term
     */
    public Annotation toAnnotation() {
        return new Annotation("source", "source(" + this.term + ")");
    }

    /**
     * Category of a source.
     */
    public enum Category {
        /** The agent itself. */
        SELF,
        /** Perception. */
        PERCEPT,
        /** Another agent. */
        AGENT,
        /** Any other source. */
        OTHER
    }
}
