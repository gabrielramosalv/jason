package jason.mind.api.core.domain.model.program;

import java.util.Objects;

public record Annotation(String functor, String term) {

    public Annotation {
        Objects.requireNonNull(functor, "functor");
        Objects.requireNonNull(term, "term");
    }

    public boolean isSource() {
        return "source".equals(functor);
    }

    @Override
    public String toString() {
        return term;
    }
}
