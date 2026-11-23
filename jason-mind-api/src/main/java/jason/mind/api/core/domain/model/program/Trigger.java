package jason.mind.api.core.domain.model.program;

import java.util.List;
import java.util.Objects;

public record Trigger(Operator operator, Type type, String literal, List<Annotation> annotations) {

    public enum Operator {
        ADD("+"), DELETE("-"), GOAL_STATE("^");

        private final String symbol;

        Operator(String symbol) { this.symbol = symbol; }

        public String symbol() { return symbol; }
    }

    public enum Type {
        BELIEF(""), ACHIEVE("!"), TEST("?"), SIGNAL("");

        private final String prefix;

        Type(String prefix) { this.prefix = prefix; }

        public String prefix() { return prefix; }
    }

    public Trigger {
        Objects.requireNonNull(operator, "operator");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(literal, "literal");
        annotations = annotations == null ? List.of() : List.copyOf(annotations);
    }

    public Trigger withOperator(Operator newOperator) {
        return new Trigger(newOperator, type, literal, annotations);
    }

    public String functor() {
        return LiteralText.functor(literal);
    }

    public int arity() {
        return LiteralText.arity(literal);
    }

    public String text() {
        return operator.symbol() + type.prefix() + literal;
    }

    @Override
    public String toString() {
        return text();
    }
}
