package jason.mind.api.core.domain.model.program;

import java.util.Objects;

public record Source(String term, Category category) {

    public enum Category { SELF, PERCEPT, AGENT, OTHER }

    public Source {
        Objects.requireNonNull(term, "term");
        Objects.requireNonNull(category, "category");
    }

    public static Source of(Annotation annotation) {
        if (!annotation.isSource()) return null;
        String t = annotation.term();
        String inner = t.startsWith("source(") && t.endsWith(")") ? t.substring(7, t.length() - 1) : t;
        return new Source(inner, categoryOf(inner));
    }

    public static Category categoryOf(String term) {
        if ("self".equals(term)) return Category.SELF;
        if ("percept".equals(term)) return Category.PERCEPT;
        if (!term.isEmpty() && Character.isLowerCase(term.charAt(0)) && term.chars().allMatch(c -> Character.isLetterOrDigit(c) || c == '_'))
            return Category.AGENT;
        return Category.OTHER;
    }

    public Annotation toAnnotation() {
        return new Annotation("source", "source(" + term + ")");
    }
}
