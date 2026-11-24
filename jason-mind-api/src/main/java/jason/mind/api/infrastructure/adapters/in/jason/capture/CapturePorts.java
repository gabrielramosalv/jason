package jason.mind.api.infrastructure.adapters.in.jason.capture;

import jason.mind.api.core.ports.in.capture.EndAgentPort;
import jason.mind.api.core.ports.in.capture.FinishCyclePort;
import jason.mind.api.core.ports.in.capture.LoadSourcesPort;
import jason.mind.api.core.ports.in.capture.RecordConsoleLinePort;
import jason.mind.api.core.ports.in.capture.RecordOccurrencePort;
import jason.mind.api.core.ports.in.capture.RecordPlanSelectionPort;
import jason.mind.api.core.ports.in.capture.StartAgentPort;
import jason.mind.api.core.ports.in.capture.StartMasPort;

/**
 * Set of input ports used by the Jason capture adapters.
 * <p>
 * Groups every capture port so that a single value can be handed to the registry and to each per-agent adapter.
 *
 * @param startMas            port that starts the recording of the multi-agent system
 * @param startAgent          port that starts the recording of one agent
 * @param endAgent            port that ends the recording of one agent
 * @param loadSources         port that loads the source files of one agent
 * @param recordOccurrence    port that records births, changes and ends of entities
 * @param recordPlanSelection port that records the plan evaluations of an option search
 * @param finishCycle         port that closes a reasoning cycle
 * @param recordConsoleLine   port that records one line of agent console output
 * @author Gabriel Lima
 */
public record CapturePorts(StartMasPort startMas, StartAgentPort startAgent, EndAgentPort endAgent,
                           LoadSourcesPort loadSources, RecordOccurrencePort recordOccurrence,
                           RecordPlanSelectionPort recordPlanSelection, FinishCyclePort finishCycle,
                           RecordConsoleLinePort recordConsoleLine) {

}
