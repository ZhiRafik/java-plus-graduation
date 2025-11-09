package ru.yandex.practicum.telemetry.analyzer.service;

import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionRequestProto;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.telemetry.analyzer.model.*;
import ru.yandex.practicum.telemetry.analyzer.repository.*;
import ru.yandex.practicum.grpc.telemetry.event.enums.*;
import ru.yandex.practicum.kafka.telemetry.event.ConditionTypeAvro;

import java.util.*;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScenarioServiceImpl implements ScenarioService {

    private final ScenarioRepository scenarioRepository;
    private final ScenarioConditionLinkRepository scenarioConditionRepository;
    private final ScenarioActionLinkRepository scenarioActionRepository;

    @Override
    public List<DeviceActionRequestProto> processSnapshot(SensorsSnapshotAvro snapshot) {
        String hubId = snapshot.getHubId();
        log.info("Обработка снапшота для хаба: {}", hubId);

        List<Scenario> scenarios = scenarioRepository.findByHubId(hubId);
        log.debug("Найдено {} сценариев для хаба {}", scenarios.size(), hubId);
        List<DeviceActionRequestProto> results = new ArrayList<>();

        for (Scenario scenario : scenarios) {
            List<ScenarioConditionLink> conditions = scenarioConditionRepository.findByScenarioId(scenario.getId());
            log.info("Найдено условий для выполнения сценария: {}", conditions.size());

            boolean allConditionsTrue = conditions.stream()
                    .allMatch(cond -> conditionMatch(cond, snapshot));

            if (allConditionsTrue) {
                log.info("Активирован сценарий {}", scenario.getName());
                List<ScenarioActionLink> actions = scenarioActionRepository.findAllByScenarioId((scenario.getId()));

                for (ScenarioActionLink actionLink : actions) {
                    Action action = actionLink.getAction();
                    Sensor sensor = actionLink.getSensor();
                    DeviceActionRequestProto deviceActionRequest = DeviceActionRequestProto.newBuilder()
                            .setHubId(snapshot.getHubId())
                            .setAction(mapDeviceAction(sensor.getId(), action))
                            .setScenarioName(scenario.getName())
                            .setTimestamp(Timestamp.newBuilder()
                                    .setSeconds(Instant.now().getEpochSecond())
                                    .setNanos(Instant.now().getNano())
                                    .build()
                            )
                            .build();
                    log.info("Должно будет быть выполнено действие типа: {}",
                            deviceActionRequest.getAction().getType());

                    results.add(deviceActionRequest);
                }
            } else {
                log.info("Сценарий '{}' не активирован", scenario.getName());
            }
        }

        return results;
    }

    public static DeviceActionProto mapDeviceAction(String sensorId, Action action) {
        DeviceActionProto.Builder builder = DeviceActionProto.newBuilder()
                .setType(mapActionType(ActionTypeAvro.valueOf(action.getType())))
                .setSensorId(sensorId);
        if (action.getValue() != null) {
            builder.setValue(action.getValue());
        }

        return builder.build();
    }

    public static ActionTypeProto mapActionType(ActionTypeAvro avro) {
        return switch (avro) {
            case ACTIVATE -> ActionTypeProto.ACTIVATE;
            case DEACTIVATE -> ActionTypeProto.DEACTIVATE;
            case INVERSE -> ActionTypeProto.INVERSE;
            case SET_VALUE -> ActionTypeProto.SET_VALUE;
        };
    }

    private boolean conditionMatch(ScenarioConditionLink con, SensorsSnapshotAvro sensorsSnapshotAvro) {

        String sensorId = con.getSensor().getId();
        Condition condition = con.getCondition();
        SensorStateAvro sensorStateAvro = sensorsSnapshotAvro.getSensorsState().get(sensorId);

        if (sensorStateAvro == null) {
            log.warn("Отстутствует значение сенсора для сенсора по id: {}", sensorId);
            return false;
        }

        Object data = sensorStateAvro.getData();

        return switch (ConditionTypeAvro.valueOf(condition.getType())) {
            case TEMPERATURE -> {
                if (data instanceof TemperatureSensorAvro temp) {
                    yield evaluateCondition(temp.getTemperatureC(), condition);
                } else if (data instanceof ClimateSensorAvro climate) {
                    yield evaluateCondition(climate.getTemperatureC(), condition);
                } else {
                    yield false;
                }
            }
            case HUMIDITY -> {
                if (data instanceof ClimateSensorAvro climate) {
                    yield evaluateCondition(climate.getHumidity(), condition);
                } else {
                    yield false;
                }
            }
            case CO2LEVEL -> {
                if (data instanceof ClimateSensorAvro climate) {
                    yield evaluateCondition(climate.getCo2Level(), condition);
                } else {
                    yield false;
                }
            }
            case LUMINOSITY -> {
                if (data instanceof LightSensorAvro light) {
                    yield evaluateCondition(light.getLuminosity(), condition);
                } else {
                    yield false;
                }
            }
            case MOTION -> {
                if (data instanceof MotionSensorAvro motion) {
                    Integer value = motion.getMotion() ? 1 : 0;
                    yield evaluateCondition(value, condition);
                } else {
                    yield false;
                }
            }
            case SWITCH -> {
                if (data instanceof SwitchSensorAvro sw) {
                    Integer value = sw.getState() ? 1 : 0;
                    yield evaluateCondition(value, condition);
                } else {
                    yield false;
                }
            }

        };
    }

    private boolean evaluateCondition(Integer value, Condition condition) {
        return switch (ConditionOperationAvro.valueOf(condition.getOperation())) {
            case EQUALS -> value == condition.getValue();
            case GREATER_THAN -> value > condition.getValue();
            case LOWER_THAN -> value < condition.getValue();
        };
    }
}