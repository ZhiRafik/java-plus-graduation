package ru.yandex.practicum.telemetry.collector.mapper;

import com.google.protobuf.util.Timestamps;
import jakarta.validation.constraints.NotNull;
import ru.yandex.practicum.grpc.telemetry.event.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.time.Instant;
import java.util.List;

public class HubEventMapper {

    public static @NotNull HubEventAvro mapToAvro(@NotNull HubEventProto proto) {
        long timestamp = Timestamps.toMillis(proto.getTimestamp());
        String hubId = proto.getHubId();

        return switch (proto.getPayloadCase()) {
            case DEVICE_ADDED -> {
                DeviceAddedEventProto e = proto.getDeviceAdded();
                DeviceAddedEventAvro payload = DeviceAddedEventAvro.newBuilder()
                        .setId(e.getId())
                        .setType(DeviceTypeAvro.valueOf(e.getType().name()))
                        .build();

                yield HubEventAvro.newBuilder()
                        .setHubId(hubId)
                        .setTimestamp(Instant.ofEpochMilli(timestamp))
                        .setPayload(payload)
                        .build();
            }

            case DEVICE_REMOVED -> {
                DeviceRemovedEventProto e = proto.getDeviceRemoved();
                DeviceRemovedEventAvro payload = DeviceRemovedEventAvro.newBuilder()
                        .setId(e.getId())
                        .build();

                yield HubEventAvro.newBuilder()
                        .setHubId(hubId)
                        .setTimestamp(Instant.ofEpochMilli(timestamp))
                        .setPayload(payload)
                        .build();
            }

            case SCENARIO_ADDED -> {
                ScenarioAddedEventProto e = proto.getScenarioAdded();

                List<ScenarioConditionAvro> conditions = e.getConditionList().stream()
                        .map(c -> {
                            ScenarioConditionAvro.Builder b = ScenarioConditionAvro.newBuilder()
                                    .setSensorId(c.getSensorId())
                                    .setType(ConditionTypeAvro.valueOf(c.getType().name()))
                                    .setOperation(ConditionOperationAvro.valueOf(c.getOperation().name()));

                            switch (c.getValueCase()) {
                                case BOOL_VALUE   -> b.setValue(c.getBoolValue());
                                case INT_VALUE    -> b.setValue(c.getIntValue());
                                default -> throw new IllegalArgumentException(
                                        "Unsupported value type: " + c.getValueCase());
                            }
                            return b.build();
                        })
                        .toList();

                List<DeviceActionAvro> actions = e.getActionList().stream()
                        .map(a -> DeviceActionAvro.newBuilder()
                                .setSensorId(a.getSensorId())
                                .setType(ActionTypeAvro.valueOf(a.getType().name()))
                                .setValue(a.getValue())
                                .build())
                        .toList();

                ScenarioAddedEventAvro payload = ScenarioAddedEventAvro.newBuilder()
                        .setName(e.getName())
                        .setConditions(conditions)
                        .setActions(actions)
                        .build();

                yield HubEventAvro.newBuilder()
                        .setHubId(hubId)
                        .setTimestamp(Instant.ofEpochMilli(timestamp))
                        .setPayload(payload)
                        .build();
            }

            case SCENARIO_REMOVED -> {
                ScenarioRemovedEventProto e = proto.getScenarioRemoved();
                ScenarioRemovedEventAvro payload = ScenarioRemovedEventAvro.newBuilder()
                        .setName(e.getName())
                        .build();

                yield HubEventAvro.newBuilder()
                        .setHubId(hubId)
                        .setTimestamp(Instant.ofEpochMilli(timestamp))
                        .setPayload(payload)
                        .build();
            }

            case PAYLOAD_NOT_SET -> throw new IllegalArgumentException(
                    "HubEventProto payload is not set or unrecognized: " + proto.getPayloadCase()
            );
        };
    }

}
