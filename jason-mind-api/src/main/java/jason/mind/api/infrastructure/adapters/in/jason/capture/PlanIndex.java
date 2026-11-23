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

final class PlanIndex {

    private final List<Deed> deeds = new ArrayList<>();
    private final Map<PlanBody, Integer> byNode = new IdentityHashMap<>();

    PlanIndex(jason.asSyntax.Plan plan) {
        add(plan.getBody(), -1);
    }

    List<Deed> deeds() {
        return deeds;
    }

    private void add(PlanBody body, int parent) {
        for (PlanBody node = body; node != null && !node.isEmptyBody(); node = node.getBodyNext()) {
            Deed.Type type = type(node.getBodyType());
            if (type == null) continue;
            int index = deeds.size();
            deeds.add(new Deed(type, text(node), line(node), parent));
            byNode.put(node, index);
            Term t = node.getBodyTerm();
            if (t instanceof Structure s && s.getArity() > 0)
                for (Term arg : s.getTerms())
                    if (arg.isPlanBody()) add((PlanBody) arg, index);
        }
    }

    Integer indexOf(PlanBody node) {
        if (node == null) return null;
        Integer i = byNode.get(node);
        if (i != null) return i;
        Deed.Type type = type(node.getBodyType());
        Integer line = line(node);
        String text = text(node);
        Integer sameLine = null;
        int sameLineCount = 0;
        for (int k = 0; k < deeds.size(); k++) {
            Deed d = deeds.get(k);
            if (d.type() != type || !java.util.Objects.equals(d.line(), line)) continue;
            if (d.content().equals(text)) return k;
            if (sameLine == null) sameLine = k;
            sameLineCount++;
        }
        return sameLineCount >= 1 ? sameLine : null;
    }

    static String text(PlanBody node) {
        return node.getBodyType().toString() + node.getBodyTerm();
    }

    static Integer line(PlanBody node) {
        SourceInfo si = node.getSrcInfo();
        if (si == null && node.getBodyTerm() != null) si = node.getBodyTerm().getSrcInfo();
        if (si == null || si.getSrcFile() == null || si.getSrcFile().isEmpty() || si.getSrcLine() <= 0) return null;
        return si.getSrcLine();
    }

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
}
