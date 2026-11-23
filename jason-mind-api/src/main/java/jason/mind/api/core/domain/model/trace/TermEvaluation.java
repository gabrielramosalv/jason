package jason.mind.api.core.domain.model.trace;

import java.util.List;

import jason.mind.api.core.domain.model.entities.Binding;

public record TermEvaluation(int index, Verdict result, List<Binding> bindings) {

    public TermEvaluation {
        bindings = bindings == null ? List.of() : List.copyOf(bindings);
    }
}
