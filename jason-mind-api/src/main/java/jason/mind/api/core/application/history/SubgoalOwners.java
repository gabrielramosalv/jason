package jason.mind.api.core.application.history;

import jason.mind.api.core.domain.model.entities.IntendedMeans;
import jason.mind.api.core.domain.model.entities.Intention;
import jason.mind.api.core.domain.model.timeline.Entity;
import jason.mind.api.core.ports.out.history.HistoryQueryRepositoryPort;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Tells which intention holds each intended means of an agent and which of them are subgoals.
 * An intended means is a subgoal when it is not the bottom of its intention's stack.
 *
 * @author Gabriel Lima
 */
public final class SubgoalOwners {

    /** Owner intention of each intended means, keyed by identity. */
    private final Map<IntendedMeans, Intention> owners;

    private SubgoalOwners(Map<IntendedMeans, Intention> owners) {
        this.owners = owners;
    }

    /**
     * Maps every intended means of an agent to the intention whose stack holds it.
     *
     * @param query read-only access to the history
     * @param agent name of the agent
     * @return the owners of the agent's intended means
     */
    public static SubgoalOwners of(HistoryQueryRepositoryPort query, String agent) {
        Map<IntendedMeans, Intention> r = new IdentityHashMap<>();
        for (Entity e : query.entitiesOfTypes(agent, Set.of("intention"))) {
            for (IntendedMeans im : ((Intention) e).getStack()) {
                r.put(im, (Intention) e);
            }
        }
        return new SubgoalOwners(r);
    }

    /**
     * Returns the intention whose stack holds an intended means.
     *
     * @param im intended means being looked up
     * @return the owner intention, or {@code null} when none holds it
     */
    public Intention ownerOf(IntendedMeans im) {
        return this.owners.get(im);
    }

    /**
     * Tells whether an intended means is a subgoal, meaning it is not the bottom of its intention's stack.
     *
     * @param im intended means being checked
     * @return {@code true} if the intended means is a subgoal
     */
    public boolean isSubgoal(IntendedMeans im) {
        Intention owner = this.owners.get(im);
        return owner != null && !owner.getStack().isEmpty() && owner.getStack().get(0) != im;
    }
}
