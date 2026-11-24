package jason.mind.api.core.ports.in.history;

/**
 * Output reference to an entity.
 *
 * @param type  type of the entity
 * @param id    identifier of the entity
 * @param label label of the entity
 * @author Gabriel Lima
 */
public record EntityRefOut(String type, String id, String label) {

}
