package ru.yandex.practicum.telemetry.analyzer.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioRemovedEventAvro;
import ru.yandex.practicum.telemetry.analyzer.repository.ScenarioRepository;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScenarioRemovedEventHandler implements HubEventHandler<ScenarioRemovedEventAvro> {
    private final ScenarioRepository scenarioRepository;

    @Override
    public void handle(ScenarioRemovedEventAvro payload, String hubId, Instant timestamp) {
        log.info("Попытка удалить сценарий: name={}, hubId={}, timestamp={}", payload.getName(), hubId, timestamp);
        scenarioRepository.findByHubIdAndName(hubId, payload.getName())
                .ifPresent(
                        s-> scenarioRepository.deleteByHubIdAndName(hubId, payload.getName())
                );
        log.info("Сценарий удалён: name={}, hubId={}", payload.getName(), hubId);
    }

    @Override
    public Class<ScenarioRemovedEventAvro> getMessageType() {
        return ScenarioRemovedEventAvro.class;
    }
}