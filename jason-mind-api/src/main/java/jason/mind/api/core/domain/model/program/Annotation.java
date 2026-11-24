package jason.mind.api.core.domain.model.program;

import java.util.Objects;

/**
 * Annotation of a literal or plan.
 * Holds the annotation functor and its full term.
 *
 * @param functor functor of the annotation
 * @param term    full text of the annotation term
 * @author Gabriel Lima
 */
public record Annotation(String functor, String term) {

    /**
     * Validates the annotation.
     *
     * @throws NullPointerException if the functor or the term is null
     */
    public Annotation {
        Objects.requireNonNull(functor, "functor");
        Objects.requireNonNull(term, "term");
    }

    /**
     * Tells whether the annotation is a source annotation.
     *
     * @return true if the functor is {@code source}
     */
    public boolean isSource() {
        return "source".equals(this.functor);
    }

    @Override
    public String toString() {
        return this.term;
    }
}
