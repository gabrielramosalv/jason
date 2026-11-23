package jason.mind.api.bootstrap;

import jason.mind.api.core.application.agent.FindAgentStateUseCase;
import jason.mind.api.core.application.capture.EndAgentUseCase;
import jason.mind.api.core.application.capture.FinishCycleUseCase;
import jason.mind.api.core.application.capture.LoadSourcesUseCase;
import jason.mind.api.core.application.capture.RecordConsoleLineUseCase;
import jason.mind.api.core.application.capture.RecordOccurrenceUseCase;
import jason.mind.api.core.application.capture.RecordPlanSelectionUseCase;
import jason.mind.api.core.application.capture.StartAgentUseCase;
import jason.mind.api.core.application.capture.StartMasUseCase;
import jason.mind.api.core.application.console.ListConsoleLinesUseCase;
import jason.mind.api.core.application.cycle.FindCycleReasoningUseCase;
import jason.mind.api.core.application.cycle.ListCyclesUseCase;
import jason.mind.api.core.application.lifecycle.FindEntityLifecycleUseCase;
import jason.mind.api.core.application.mas.FindMasUseCase;
import jason.mind.api.core.application.message.ListMessagesUseCase;
import jason.mind.api.core.application.plan.FindPlansUseCase;
import jason.mind.api.core.application.search.SearchDeedExecutionsUseCase;
import jason.mind.api.core.application.search.SearchEntitiesUseCase;
import jason.mind.api.core.application.settings.FindSettingsUseCase;
import jason.mind.api.core.application.settings.UpdateSettingsUseCase;
import jason.mind.api.core.application.source.FindSourcesUseCase;
import jason.mind.api.core.domain.service.history.EffectsDomainService;
import jason.mind.api.core.domain.service.history.HistoryRecorderDomainService;
import jason.mind.api.core.domain.service.reasoning.CandidatesDomainService;
import jason.mind.api.core.domain.service.reasoning.QueueDomainService;
import jason.mind.api.core.ports.out.clock.ClockProviderPort;
import jason.mind.api.core.ports.out.console.ConsoleRepositoryPort;
import jason.mind.api.core.ports.out.history.HistoryQueryRepositoryPort;
import jason.mind.api.core.ports.out.history.HistoryRepositoryPort;
import jason.mind.api.core.ports.out.mas.MasRepositoryPort;
import jason.mind.api.core.ports.out.sequence.EntityIdProviderPort;
import jason.mind.api.core.ports.out.sequence.SequenceProviderPort;
import jason.mind.api.core.ports.out.settings.SettingsRepositoryPort;
import jason.mind.api.core.ports.out.source.SourceRepositoryPort;
import jason.mind.api.infrastructure.adapters.in.jason.capture.CapturePorts;
import jason.mind.api.infrastructure.adapters.in.jason.capture.JasonCaptureRegistry;
import jason.mind.api.infrastructure.adapters.in.web.agent.AgentStateController;
import jason.mind.api.infrastructure.adapters.in.web.console.ConsoleController;
import jason.mind.api.infrastructure.adapters.in.web.cycle.CycleController;
import jason.mind.api.infrastructure.adapters.in.web.lifecycle.LifecycleController;
import jason.mind.api.infrastructure.adapters.in.web.mas.MasController;
import jason.mind.api.infrastructure.adapters.in.web.message.MessageController;
import jason.mind.api.infrastructure.adapters.in.web.plan.PlanController;
import jason.mind.api.infrastructure.adapters.in.web.search.SearchController;
import jason.mind.api.infrastructure.adapters.in.web.settings.SettingsController;
import jason.mind.api.infrastructure.adapters.in.web.source.SourceController;
import jason.mind.api.infrastructure.adapters.out.persistence.memory.console.ConsoleRepositoryAdapter;
import jason.mind.api.infrastructure.adapters.out.persistence.memory.history.HistoryMemory;
import jason.mind.api.infrastructure.adapters.out.persistence.memory.history.HistoryQueryRepositoryAdapter;
import jason.mind.api.infrastructure.adapters.out.persistence.memory.history.HistoryRepositoryAdapter;
import jason.mind.api.infrastructure.adapters.out.persistence.memory.mas.MasRepositoryAdapter;
import jason.mind.api.infrastructure.adapters.out.persistence.memory.settings.SettingsRepositoryAdapter;
import jason.mind.api.infrastructure.adapters.out.persistence.memory.source.SourceRepositoryAdapter;
import jason.mind.api.infrastructure.adapters.out.provider.memory.sequence.EntityIdProviderAdapter;
import jason.mind.api.infrastructure.adapters.out.provider.memory.sequence.SequenceProviderAdapter;
import jason.mind.api.infrastructure.adapters.out.provider.system.clock.ClockProviderAdapter;

import java.util.List;
import java.util.function.Supplier;

public final class MindApiModule {

    public static final String MIND_VERSION = "2.0.0-SNAPSHOT";

    private final JasonCaptureRegistry captureRegistry;
    private final List<Object> controllers;

    public MindApiModule(String jasonVersion, Supplier<String> masName) {
        HistoryMemory memory = new HistoryMemory();
        HistoryRepositoryPort history = new HistoryRepositoryAdapter(memory);
        HistoryQueryRepositoryPort historyQuery = new HistoryQueryRepositoryAdapter(memory);
        ConsoleRepositoryPort console = new ConsoleRepositoryAdapter();
        MasRepositoryPort mas = new MasRepositoryAdapter();
        SettingsRepositoryPort settings = new SettingsRepositoryAdapter();
        SourceRepositoryPort sources = new SourceRepositoryAdapter();
        ClockProviderPort clock = new ClockProviderAdapter();
        SequenceProviderPort sequence = new SequenceProviderAdapter();
        EntityIdProviderPort entityIds = new EntityIdProviderAdapter();
        QueueDomainService queue = new QueueDomainService();

        CapturePorts capture = new CapturePorts(
                new StartMasUseCase(mas),
                new StartAgentUseCase(history, sequence),
                new EndAgentUseCase(history, sequence),
                new LoadSourcesUseCase(sources),
                new RecordOccurrenceUseCase(history, sequence, entityIds, new HistoryRecorderDomainService()),
                new RecordPlanSelectionUseCase(history, sequence),
                new FinishCycleUseCase(history, clock, sequence, console, settings),
                new RecordConsoleLineUseCase(console, clock, sequence));
        this.captureRegistry = new JasonCaptureRegistry(capture, masName);

        this.controllers = List.of(
                new MasController(new FindMasUseCase(historyQuery, mas, MIND_VERSION, jasonVersion)),
                new AgentStateController(new FindAgentStateUseCase(historyQuery, queue)),
                new CycleController(new ListCyclesUseCase(historyQuery),
                        new FindCycleReasoningUseCase(historyQuery, new CandidatesDomainService(), queue)),
                new LifecycleController(new FindEntityLifecycleUseCase(historyQuery, new EffectsDomainService())),
                new MessageController(new ListMessagesUseCase(historyQuery)),
                new ConsoleController(new ListConsoleLinesUseCase(console, historyQuery)),
                new SourceController(new FindSourcesUseCase(sources, historyQuery)),
                new PlanController(new FindPlansUseCase(historyQuery)),
                new SearchController(new SearchEntitiesUseCase(historyQuery), new SearchDeedExecutionsUseCase(historyQuery)),
                new SettingsController(new FindSettingsUseCase(settings, historyQuery), new UpdateSettingsUseCase(settings, historyQuery)));
    }

    public JasonCaptureRegistry captureRegistry() {
        return captureRegistry;
    }

    public List<Object> controllers() {
        return controllers;
    }
}
