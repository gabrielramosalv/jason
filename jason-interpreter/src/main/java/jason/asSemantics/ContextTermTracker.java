package jason.asSemantics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import jason.asSyntax.LogExpr;
import jason.asSyntax.LogicalFormula;
import jason.asSyntax.Term;
import jason.asSyntax.VarTerm;

public final class ContextTermTracker {

    private final List<LogicalFormula> terms;
    private final int[] attempts;
    private final int[] successes;
    private final List<List<String[]>> bindings = new ArrayList<>();

    public ContextTermTracker(LogicalFormula context) {
        this.terms = new ArrayList<>();
        flatten(context, terms);
        this.attempts = new int[terms.size()];
        this.successes = new int[terms.size()];
        for (int i = 0; i < terms.size(); i++)
            bindings.add(null);
    }

    public static List<LogicalFormula> topLevelTerms(LogicalFormula context) {
        List<LogicalFormula> r = new ArrayList<>();
        if (context != null)
            flatten(context, r);
        return r;
    }

    private static void flatten(LogicalFormula f, List<LogicalFormula> out) {
        if (f instanceof LogExpr && ((LogExpr) f).getOp() == LogExpr.LogicalOp.and) {
            flatten(((LogExpr) f).getLHS(), out);
            flatten(((LogExpr) f).getRHS(), out);
        } else {
            out.add(f);
        }
    }

    public Iterator<Unifier> logicalConsequence(Agent ag, Unifier un) {
        return chain(0, ag, un);
    }

    private Iterator<Unifier> chain(int index, Agent ag, Unifier un) {
        Iterator<Unifier> it = tracked(index, ag, un);
        if (index == terms.size() - 1)
            return it;

        return new Iterator<Unifier>() {
            Iterator<Unifier> right = null;
            Unifier current = null;
            boolean needsUpdate = true;

            public boolean hasNext() {
                if (needsUpdate) get();
                return current != null;
            }

            public Unifier next() {
                if (needsUpdate) get();
                if (current == null) throw new NoSuchElementException();
                needsUpdate = true;
                return current;
            }

            private void get() {
                needsUpdate = false;
                current = null;
                while ((right == null || !right.hasNext()) && it.hasNext())
                    right = chain(index + 1, ag, it.next());
                if (right != null && right.hasNext())
                    current = right.next();
            }
        };
    }

    private Iterator<Unifier> tracked(int index, Agent ag, Unifier un) {
        LogicalFormula term = terms.get(index);
        attempts[index]++;
        Iterator<Unifier> it = term.logicalConsequence(ag, un);
        boolean has = it != null && it.hasNext();
        if (!has)
            return LogExpr.EMPTY_UNIF_LIST.iterator();
        successes[index]++;
        if (bindings.get(index) == null) {

            return new Iterator<Unifier>() {
                boolean first = true;
                public boolean hasNext() { return it.hasNext(); }
                public Unifier next() {
                    Unifier u = it.next();
                    if (first) {
                        first = false;
                        bindings.set(index, bindingsOf(term, u));
                    }
                    return u;
                }
            };
        }
        return it;
    }

    private static List<String[]> bindingsOf(Term term, Unifier u) {
        Map<VarTerm, Integer> vars = new LinkedHashMap<>();
        term.countVars(vars);
        List<String[]> r = new ArrayList<>();
        for (VarTerm v : vars.keySet()) {
            if (v.isUnnamedVar())
                continue;
            Term value = u.get(v);
            if (value != null)
                r.add(new String[] { v.toString(), value.capply(u).toString() });
        }
        return r;
    }

    public List<ReasoningListener.TermResult> results() {
        List<ReasoningListener.TermResult> r = new ArrayList<>();
        for (int i = 0; i < terms.size(); i++) {
            List<String[]> b = bindings.get(i);
            r.add(new ReasoningListener.TermResult(i, attempts[i], successes[i], b == null ? List.of() : b));
        }
        return r;
    }
}
