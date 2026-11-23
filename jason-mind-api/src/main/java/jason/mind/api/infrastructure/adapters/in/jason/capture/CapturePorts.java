package jason.mind.api.infrastructure.adapters.in.jason.capture;

import jason.mind.api.core.ports.in.capture.EndAgentPort;
import jason.mind.api.core.ports.in.capture.FinishCyclePort;
import jason.mind.api.core.ports.in.capture.LoadSourcesPort;
import jason.mind.api.core.ports.in.capture.RecordConsoleLinePort;
import jason.mind.api.core.ports.in.capture.RecordOccurrencePort;
import jason.mind.api.core.ports.in.capture.RecordPlanSelectionPort;
import jason.mind.api.core.ports.in.capture.StartAgentPort;
import jason.mind.api.core.ports.in.capture.StartMasPort;

public record CapturePorts(StartMasPort startMas, StartAgentPort startAgent, EndAgentPort endAgent,
                           LoadSourcesPort loadSources, RecordOccurrencePort recordOccurrence,
                           RecordPlanSelectionPort recordPlanSelection, FinishCyclePort finishCycle,
                           RecordConsoleLinePort recordConsoleLine) {
}
