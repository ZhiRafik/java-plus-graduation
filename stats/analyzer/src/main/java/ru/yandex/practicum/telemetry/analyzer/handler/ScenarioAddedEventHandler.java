package ru.yandex.practicum.telemetry.analyzer.handler;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioAddedEventAvro;
import ru.yandex.practicum.telemetry.analyzer.model.*;
import ru.yandex.practicum.telemetry.analyzer.repository.*;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScenarioAddedEventHandler implements HubEventHandler<ScenarioAddedEventAvro> {

    private final ScenarioRepository scenarioRepository;
    private final ActionRepository actionRepository;
    private final ConditionRepository conditionRepository;
    private final ScenarioConditionLinkRepository scenarioConditionLinkRepository;
    private final ScenarioActionLinkRepository scenarioActionLinkRepository;

    @Transactional
    @Override
    public void handle(ScenarioAddedEventAvro payload, String hubId, Instant timestamp) {
        log.info("Обработка ScenarioAddedEvent: name={}, hubId={}, timestamp={}", payload.getName(), hubId, timestamp);
        log.debug("Payload: {}", payload);

        Scenario scenarioEntity = new Scenario();
        scenarioEntity.setHubId(hubId);
        scenarioEntity.setName(payload.getName());

        List<Condition> conditionEntityList = payload.getConditions().stream()
                .map(avroCondition -> Condition.builder()
                        .type(avroCondition.getType().toString())
                        .value(switch (avroCondition.getValue()) {
                            case null -> null;
                            case Integer intValue -> intValue;
                            case Boolean boolValue -> boolValue ? 1 : 0;
                            default -> throw new IllegalArgumentException(
                                    "Неподдерживаемый тип условий: " + avroCondition.getValue().getClass());
                        })
                        .operation(avroCondition.getOperation().toString())
                        .build())
                .toList();

        List<ScenarioConditionLink> conditionLinks = payload.getConditions().stream()
                .map(avroCondition -> {

                    String sensorId = avroCondition.getSensorId();
                    Sensor sensor = Sensor.builder()
                            .id(sensorId)
                            .hubId(hubId)
                            .build();

                    Condition condition = conditionEntityList.get(payload.getConditions().indexOf(avroCondition));

                    return ScenarioConditionLink.builder()
                            .id(new ScenarioConditionLink.ScenarioConditionId(
                                    scenarioEntity.getId(),
                                    sensor.getId(),
                                    condition.getId()
                            ))
                            .scenario(scenarioEntity)
                            .sensor(sensor)
                            .condition(condition)
                            .build();
                })
                .toList();

        List<Action> actionEntityList = payload.getActions().stream()
                .map(avroAction -> Action.builder()
                        .type(avroAction.getType().toString())
                        .value(avroAction.getValue())
                        .build())
                .toList();

        List<ScenarioActionLink> actionLinks = payload.getActions().stream()
                .map(avroAction -> {
                    String sensorId = avroAction.getSensorId();
                    Sensor sensor = Sensor.builder()
                            .id(sensorId)
                            .hubId(hubId)
                            .build();
                    Action Action = actionEntityList.get(payload.getActions().indexOf(avroAction));
                    return ScenarioActionLink.builder()
                            .id(new ScenarioActionLink.ScenarioActionId(
                                    scenarioEntity.getId(),
                                    sensor.getId(),
                                    Action.getId()
                            ))
                            .scenario(scenarioEntity)
                            .sensor(sensor)
                            .action(Action)
                            .build();
                })
                .toList();

        scenarioRepository.save(scenarioEntity);
        log.info("Сценарий сохранён: name={}, hubId={}", scenarioEntity.getName(), hubId);

        conditionRepository.saveAll(conditionEntityList);
        log.info("Условий сохранено: {}", conditionEntityList.size());

        actionRepository.saveAll(actionEntityList);
        log.info("Действий сохранено: {}", actionEntityList.size());

        scenarioConditionLinkRepository.saveAll(conditionLinks);
        log.info("Связей сценарий-условие сохранено: {}", conditionLinks.size());

        scenarioActionLinkRepository.saveAll(actionLinks);
        log.info("Связей сценарий-действие сохранено: {}", actionLinks.size());

    }

    @Override
    public Class<ScenarioAddedEventAvro> getMessageType() {
        return ScenarioAddedEventAvro.class;
    }
}