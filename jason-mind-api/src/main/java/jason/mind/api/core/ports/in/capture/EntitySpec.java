package jason.mind.api.core.ports.in.capture;

import jason.mind.api.core.domain.model.entities.Event;
import jason.mind.api.core.domain.model.entities.Intention;
import jason.mind.api.core.domain.model.entities.Performative;
import jason.mind.api.core.domain.model.entities.SendMode;
import jason.mind.api.core.domain.model.program.Annotation;
import jason.mind.api.core.domain.model.program.Deed;
import jason.mind.api.core.domain.model.program.Source;
import jason.mind.api.core.domain.model.program.SourceLocation;
import jason.mind.api.core.domain.model.program.Trigger;

import java.util.List;

/**
 * Specification of an entity to be created.
 * Each implementation carries the data needed to create one kind of entity.
 *
 * @author Gabriel Lima
 */
public sealed interface EntitySpec {

    /**
     * Specification of a belief.
     *
     * @param content     text of the belief
     * @param rule        whether it is a rule
     * @param orderKey    key that orders the belief in the base
     * @param annotations initial annotations
     */
    record BeliefSpec(String content, boolean rule, long orderKey, List<Annotation> annotations) implements EntitySpec {

    }

    /**
     * Specification of an event.
     *
     * @param trigger trigger of the event
     * @param atomic  whether the event is atomic
     * @param place   initial place of the event
     */
    record EventSpec(Trigger trigger, boolean atomic, Event.Place place) implements EntitySpec {

    }

    /**
     * Specification of an intention.
     *
     * @param place initial place of the intention
     */
    record IntentionSpec(Intention.Place place) implements EntitySpec {

    }

    /**
     * Specification of an intended means.
     *
     * @param planId      identifier of the instantiated plan
     * @param intentionId identifier of the owning intention
     */
    record IntendedMeansSpec(long planId, long intentionId) implements EntitySpec {

    }

    /**
     * Specification of a plan.
     *
     * @param label       explicit label, or null
     * @param orderKey    key that orders the plan in the library
     * @param trigger     triggering event
     * @param context     terms of the context
     * @param body        deeds of the body
     * @param annotations annotations of the label
     * @param source      location in the source code
     * @param sources     initial sources
     */
    record PlanSpec(String label, long orderKey, Trigger trigger, List<String> context, List<Deed> body,
                    List<Annotation> annotations, SourceLocation source, List<Source> sources) implements EntitySpec {

    }

    /**
     * Specification of an action.
     *
     * @param action text of the action
     */
    record ActionSpec(String action) implements EntitySpec {

    }

    /**
     * Specification of a message.
     *
     * @param sendNumber   number of the send operation
     * @param sendMode     mode of the send operation
     * @param inReplyToId  identifier of the message being replied, or null
     * @param senderName   name of the sender
     * @param receiver     name of the receiver
     * @param performative performative of the message
     * @param content      content of the message
     */
    record MessageSpec(Integer sendNumber, SendMode sendMode, Long inReplyToId, String senderName, String receiver,
                       Performative performative, String content) implements EntitySpec {

    }

    /**
     * Specification of a timer.
     *
     * @param trigger              trigger fired on expiration
     * @param deadlineMilliseconds deadline, in milliseconds
     */
    record TimerSpec(Trigger trigger, long deadlineMilliseconds) implements EntitySpec {

    }
}
