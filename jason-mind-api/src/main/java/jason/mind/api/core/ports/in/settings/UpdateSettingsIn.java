package jason.mind.api.core.ports.in.settings;

/**
 * Input of the update of the history settings.
 *
 * @param retentionCycles new number of retained cycles, or null to keep the current one
 * @author Gabriel Lima
 */
public record UpdateSettingsIn(Long retentionCycles) {

}
