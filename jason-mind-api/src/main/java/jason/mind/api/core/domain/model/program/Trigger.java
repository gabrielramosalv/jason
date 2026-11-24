package jason.mind.api.core.domain.model.program;

import java.util.List;
import java.util.Objects;

/**
 * Triggering event of a plan or event.
 * Holds the operator, the type, the literal and its annotations.
 *
 * @param operator    operator of the trigger
 * @param type        type of the trigger
 * @param literal     text of the literal
 * @param annotations annotations of the literal
 * @author Gabriel Lima
 */
public record Trigger(Operator operator, Type type, String literal, List<Annotation> annotations) {

    /**
     * Validates the trigger and copies the annotations, using an empty list when they are null.
     *
     * @throws NullPointerException if the operator, the type or the literal is null
     */
    public Trigger {
        Objects.requireNonNull(operator, "operator");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(literal, "literal");
        annotations = annotations == null ? List.of() : List.copyOf(annotations);
    }

    /**
     * Returns a copy of the trigger with another operator.
     *
     * @param newOperator operator of the copy
     * @return trigger equal to this one except for the operator
     */
    public Trigger withOperator(Operator newOperator) {
        return new Trigger(newOperator, this.type, this.literal, this.annotations);
    }

    /**
     * Returns the functor of the literal.
     *
     * @return functor of the literal
     */
    public String functor() {
        return LiteralText.functor(this.literal);
    }

    /**
     * Returns the arity of the literal.
     *
     * @return number of arguments of the literal
     */
    public int arity() {
        return LiteralText.arity(this.literal);
    }

    /**
     * Returns the textual form of the trigger.
     *
     * @return operator symbol, type prefix and literal
     */
    public String text() {
        return this.operator.symbol() + this.type.prefix() + this.literal;
    }

    @Override
    public String toString() {
        return this.text();
    }

    /**
     * Operator of a trigger.
     * Each operator has the symbol written before the trigger.
     */
    public enum Operator {
        /** Addition. */
        ADD("+"),
        /** Deletion. */
        DELETE("-"),
        /** Goal state change. */
        GOAL_STATE("^");

        /** Symbol of the operator. */
        private final String symbol;

        /**
         * Creates the operator with its symbol.
         *
         * @param symbol {@link #symbol}
         */
        Operator(String symbol) {
            this.symbol = symbol;
        }

        /**
         * Returns the symbol of the operator.
         *
         * @return symbol written before the trigger
         */
        public String symbol() {
            return this.symbol;
        }
    }

    /**
     * Type of a trigger.
     * Each type has the prefix written before the literal.
     */
    public enum Type {
        /** Belief. */
        BELIEF(""),
        /** Achievement goal. */
        ACHIEVE("!"),
        /** Test goal. */
        TEST("?"),
        /** Signal. */
        SIGNAL("");

        /** Prefix of the type. */
        private final String prefix;

        /**
         * Creates the type with its prefix.
         *
         * @param prefix {@link #prefix}
         */
        Type(String prefix) {
            this.prefix = prefix;
        }

        /**
         * Returns the prefix of the type.
         *
         * @return prefix written before the literal
         */
        public String prefix() {
            return this.prefix;
        }
    }
}
