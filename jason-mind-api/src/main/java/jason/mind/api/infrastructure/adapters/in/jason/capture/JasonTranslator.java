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

/**
 * Translator from Jason syntax elements to Mind domain values.
 * <p>
 * Converts triggers, literals, annotations and plan contexts into their domain representation.
 *
 * @author Gabriel Lima
 */
final class JasonTranslator {

    /**
     * Prevents instantiation of this utility class.
     */
    private JasonTranslator() {
    }

    /**
     * Converts a Jason trigger into a domain trigger.
     *
     * @param t Jason trigger to convert
     * @return the domain trigger with operator, type, literal text and annotations
     */
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

    /**
     * Renders a literal without its annotations.
     *
     * @param l literal to render
     * @return the text of the literal with its annotations removed; the literal itself is left untouched
     */
    static String withoutAnnotations(Literal l) {
        if (!l.hasAnnot()) {
            return l.toString();
        }
        Literal c = l.copy();
        c.clearAnnots();
        return c.toString();
    }

    /**
     * Converts the annotations of a literal.
     *
     * @param l literal whose annotations are converted
     * @return the domain annotations in declaration order, or an empty list when the literal has none
     */
    static List<Annotation> annotations(Literal l) {
        if (!l.hasAnnot()) {
            return List.of();
        }
        List<Annotation> r = new ArrayList<>();
        for (Term t : l.getAnnots()) {
            r.add(annotation(t));
        }
        return r;
    }

    /**
     * Converts one annotation term.
     *
     * @param t annotation term to convert
     * @return the domain annotation, named by the structure functor when the term is a structure and by its full
     * text otherwise
     */
    static Annotation annotation(Term t) {
        String functor = t instanceof Structure s ? s.getFunctor() : t.toString();
        return new Annotation(functor, t.toString());
    }

    /**
     * Extracts the sources from a list of annotations.
     *
     * @param annotations annotations to inspect
     * @return the sources declared by the annotations, in order
     */
    static List<Source> sources(List<Annotation> annotations) {
        List<Source> r = new ArrayList<>();
        for (Annotation a : annotations) {
            Source s = Source.of(a);
            if (s != null) {
                r.add(s);
            }
        }
        return r;
    }

    /**
     * Splits a plan context into its top-level terms.
     *
     * @param context plan context to split
     * @return the text of each top-level term of the context
     */
    static List<String> context(LogicalFormula context) {
        List<String> r = new ArrayList<>();
        for (LogicalFormula f : ContextTermTracker.topLevelTerms(context)) {
            r.add(f.toString());
        }
        return r;
    }
}
