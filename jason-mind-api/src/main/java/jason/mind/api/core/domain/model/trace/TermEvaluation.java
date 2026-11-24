package jason.mind.api.core.domain.model.trace;

import jason.mind.api.core.domain.model.entities.Binding;

import java.util.List;

/**
 * Evaluation of a term of a plan context.
 * Holds its position, its verdict and the bindings it produced.
 *
 * @param index    position of the term in the context
 * @param result   verdict of the term
 * @param bindings bindings produced by the term
 * @author Gabriel Lima
 */
public record TermEvaluation(int index, Verdict result, List<Binding> bindings) {

    /**
     * Copies the bindings, using an empty list when they are null.
     */
    public TermEvaluation {
        bindings = bindings == null ? List.of() : List.copyOf(bindings);
    }
}
