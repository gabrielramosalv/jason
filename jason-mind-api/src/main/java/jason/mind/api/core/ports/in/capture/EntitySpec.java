package jason.mind.api.core.ports.in.capture;

import java.util.List;

import jason.mind.api.core.domain.model.entities.Event;
import jason.mind.api.core.domain.model.entities.Intention;
import jason.mind.api.core.domain.model.entities.Performative;
import jason.mind.api.core.domain.model.entities.SendMode;
import jason.mind.api.core.domain.model.program.Annotation;
import jason.mind.api.core.domain.model.program.Deed;
import jason.mind.api.core.domain.model.program.Source;
import jason.mind.api.core.domain.model.program.SourceLocation;
import jason.mind.api.core.domain.model.program.Trigger;

public sealed interface EntitySpec {

    record BeliefSpec(String content, boolean rule, long orderKey, List<Annotation> annotations) implements EntitySpec {
    }

    record EventSpec(Trigger trigger, boolean atomic, Event.Place place) implements EntitySpec {
    }

    record IntentionSpec(Intention.Place place) implements EntitySpec {
    }

    record IntendedMeansSpec(long planId, long intentionId) implements EntitySpec {
    }

    record PlanSpec(String label, long orderKey, Trigger trigger, List<String> context, List<Deed> body,
                    List<Annotation> annotations, SourceLocation source, List<Source> sources) implements EntitySpec {
    }

    record ActionSpec(String action) implements EntitySpec {
    }

    record MessageSpec(Integer sendNumber, SendMode sendMode, Long inReplyToId, String senderName, String receiver,
                       Performative performative, String content) implements EntitySpec {
    }

    record TimerSpec(Trigger trigger, long deadlineMilliseconds) implements EntitySpec {
    }
}
