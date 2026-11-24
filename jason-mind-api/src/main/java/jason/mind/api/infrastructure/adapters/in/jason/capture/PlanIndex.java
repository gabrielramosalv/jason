package jason.mind.api.infrastructure.adapters.in.jason.capture;

import jason.asSyntax.PlanBody;
import jason.asSyntax.SourceInfo;
import jason.asSyntax.Structure;
import jason.asSyntax.Term;
import jason.mind.api.core.domain.model.program.Deed;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Index of the deeds of a Jason plan body.
 * <p>
 * Flattens the plan body, including nested plan bodies passed as arguments, into a list of domain deeds and resolves
 * a body node back to its deed index.
 *
 * @author Gabriel Lima
 */
final class PlanIndex {

    /** Deeds of the plan in depth-first order. */
    private final List<Deed> deeds = new ArrayList<>();

    /** Deed index of each indexed body node, compared by identity. */
    private final Map<PlanBody, Integer> byNode = new IdentityHashMap<>();

    /**
     * Creates the index with the deeds of the given plan.
     *
     * @param plan Jason plan whose body is indexed
     */
    PlanIndex(jason.asSyntax.Plan plan) {
        this.add(plan.getBody(), -1);
    }

    /**
     * Renders a body node as deed text.
     *
     * @param node body node to render
     * @return the body type symbol followed by the body term
     */
    static String text(PlanBody node) {
        return node.getBodyType().toString() + node.getBodyTerm();
    }

    /**
     * Finds the source line of a body node, falling back to the line of its body term.
     *
     * @param node body node to inspect
     * @return the positive source line, or {@code null} when there is no source file or line
     */
    static Integer line(PlanBody node) {
        SourceInfo si = node.getSrcInfo();
        if (si == null && node.getBodyTerm() != null) {
            si = node.getBodyTerm().getSrcInfo();
        }
        if (si == null || si.getSrcFile() == null || si.getSrcFile().isEmpty() || si.getSrcLine() <= 0) {
            return null;
        }
        return si.getSrcLine();
    }

    /**
     * Converts a Jason body type into a deed type.
     *
     * @param t Jason body type to convert
     * @return the deed type, or {@code null} for body types that are not recorded as deeds
     */
    static Deed.Type type(PlanBody.BodyType t) {
        return switch (t) {
            case action -> Deed.Type.ACTION;
            case internalAction -> Deed.Type.INTERNAL_ACTION;
            case achieve -> Deed.Type.ACHIEVE;
            case achieveNF -> Deed.Type.ACHIEVE_NEW_FOCUS;
            case test -> Deed.Type.TEST;
            case addBel -> Deed.Type.ADD_BELIEF;
            case addBelNewFocus -> Deed.Type.ADD_BELIEF_NEW_FOCUS;
            case addBelBegin -> Deed.Type.ADD_BELIEF_BEGIN;
            case addBelEnd -> Deed.Type.ADD_BELIEF_END;
            case delBel -> Deed.Type.DEL_BELIEF;
            case delBelNewFocus -> Deed.Type.DEL_BELIEF_NEW_FOCUS;
            case delAddBel -> Deed.Type.DEL_ADD_BELIEF;
            case constraint -> Deed.Type.CONSTRAINT;
            default -> null;
        };
    }

    /**
     * Returns the indexed deeds.
     *
     * @return the deeds of the plan in depth-first order
     */
    List<Deed> deeds() {
        return this.deeds;
    }

    /**
     * Resolves a body node to its deed index.
     * <p>
     * Uses the node identity first; otherwise matches by type and line, preferring a deed with the same text and
     * falling back to the first deed on the same line.
     *
     * @param node body node to resolve
     * @return the deed index, or {@code null} when the node is {@code null} or no deed matches
     */
    Integer indexOf(PlanBody node) {
        if (node == null) {
            return null;
        }
        Integer i = this.byNode.get(node);
        if (i != null) {
            return i;
        }
        Deed.Type type = type(node.getBodyType());
        Integer line = line(node);
        String text = text(node);
        Integer sameLine = null;
        int sameLineCount = 0;
        for (int k = 0; k < this.deeds.size(); k++) {
            Deed d = this.deeds.get(k);
            if (d.type() != type || !Objects.equals(d.line(), line)) {
                continue;
            }
            if (d.content().equals(text)) {
                return k;
            }
            if (sameLine == null) {
                sameLine = k;
            }
            sameLineCount++;
        }
        return sameLineCount >= 1 ? sameLine : null;
    }

    /**
     * Indexes the deeds of a body and, recursively, of plan bodies passed as arguments.
     *
     * @param body   first node of the body to index
     * @param parent index of the deed that contains the body, or {@code -1} for the plan body itself
     */
    private void add(PlanBody body, int parent) {
        for (PlanBody node = body; node != null && !node.isEmptyBody(); node = node.getBodyNext()) {
            Deed.Type type = type(node.getBodyType());
            if (type == null) {
                continue;
            }
            int index = this.deeds.size();
            this.deeds.add(new Deed(type, text(node), line(node), parent));
            this.byNode.put(node, index);
            Term t = node.getBodyTerm();
            if (t instanceof Structure s && s.getArity() > 0) {
                for (Term arg : s.getTerms()) {
                    if (arg.isPlanBody()) {
                        this.add((PlanBody) arg, index);
                    }
                }
            }
        }
    }
}
