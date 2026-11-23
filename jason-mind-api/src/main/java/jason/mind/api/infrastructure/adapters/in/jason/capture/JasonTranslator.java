package jason.mind.api.infrastructure.adapters.in.jason.capture;

import jason.asSemantics.ContextTermTracker;
import jason.asSyntax.Literal;
import jason.asSyntax.LogicalFormula;
import jason.asSyntax.Structure;
import jason.asSyntax.Term;
import jason.mind.api.core.domain.model.program.Annotation;
import jason.mind.api.core.domain.model.program.Source;
import jason.mind.api.core.domain.model.program.Trigger;

import java.util.ArrayList;
import java.util.List;

final class JasonTranslator {

    private JasonTranslator() {
    }

    static Trigger trigger(jason.asSyntax.Trigger t) {
        Trigger.Operator op = switch (t.getOperator()) {
            case add -> Trigger.Operator.ADD;
            case del -> Trigger.Operator.DELETE;
            case goalState -> Trigger.Operator.GOAL_STATE;
        };
        Trigger.Type type = switch (t.getType()) {
            case belief -> Trigger.Type.BELIEF;
            case achieve -> Trigger.Type.ACHIEVE;
            case test -> Trigger.Type.TEST;
            default -> Trigger.Type.SIGNAL;
        };
        Literal l = t.getLiteral();
        return new Trigger(op, type, withoutAnnotations(l), annotations(l));
    }

    static String withoutAnnotations(Literal l) {
        if (!l.hasAnnot()) return l.toString();
        Literal c = l.copy();
        c.clearAnnots();
        return c.toString();
    }

    static List<Annotation> annotations(Literal l) {
        if (!l.hasAnnot()) return List.of();
        List<Annotation> r = new ArrayList<>();
        for (Term t : l.getAnnots()) r.add(annotation(t));
        return r;
    }

    static Annotation annotation(Term t) {
        String functor = t instanceof Structure s ? s.getFunctor() : t.toString();
        return new Annotation(functor, t.toString());
    }

    static List<Source> sources(List<Annotation> annotations) {
        List<Source> r = new ArrayList<>();
        for (Annotation a : annotations) {
            Source s = Source.of(a);
            if (s != null) r.add(s);
        }
        return r;
    }

    static List<String> context(LogicalFormula context) {
        List<String> r = new ArrayList<>();
        for (LogicalFormula f : ContextTermTracker.topLevelTerms(context)) r.add(f.toString());
        return r;
    }
}
