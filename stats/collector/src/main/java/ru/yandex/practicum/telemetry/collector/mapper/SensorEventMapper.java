package ru.yandex.practicum.telemetry.collector.mapper;

import com.google.protobuf.util.Timestamps;
import jakarta.validation.constraints.NotNull;
import ru.yandex.practicum.grpc.telemetry.event.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.time.Instant;

public class SensorEventMapper {

    public static @NotNull SensorEventAvro mapToAvro(@NotNull SensorEventProto proto) {
        long timestamp = Timestamps.toMillis(proto.getTimestamp());
        String id = proto.getId();
        String hubId = proto.getHubId();

        return switch (proto.getPayloadCase()) {
            case TEMPERATURE_SENSOR_EVENT -> {
                TemperatureSensorProto e = proto.getTemperatureSensorEvent();
                TemperatureSensorAvro payload = TemperatureSensorAvro.newBuilder()
                        .setTemperatureC(e.getTemperatureC())
                        .setTemperatureF(e.getTemperatureF())
                        .build();

                yield SensorEventAvro.newBuilder()
                        .setId(id)
                        .setHubId(hubId)
                        .setTimestamp(Instant.ofEpochMilli(timestamp))
                        .setPayload(payload)
                        .build();
            }

            case CLIMATE_SENSOR_EVENT -> {
                ClimateSensorProto e = proto.getClimateSensorEvent();
                ClimateSensorAvro payload = ClimateSensorAvro.newBuilder()
                        .setTemperatureC(e.getTemperatureC())
                        .setHumidity(e.getHumidity())
                        .setCo2Level(e.getCo2Level())
                        .build();

                yield SensorEventAvro.newBuilder()
                        .setId(id)
                        .setHubId(hubId)
                        .setTimestamp(Instant.ofEpochMilli(timestamp))
                        .setPayload(payload)
                        .build();
            }

            case LIGHT_SENSOR_EVENT -> {
                LightSensorProto e = proto.getLightSensorEvent();
                LightSensorAvro payload = LightSensorAvro.newBuilder()
                        .setLuminosity(e.getLuminosity())
                        .setLinkQuality(e.getLinkQuality())
                        .build();

                yield SensorEventAvro.newBuilder()
                        .setId(id)
                        .setHubId(hubId)
                        .setTimestamp(Instant.ofEpochMilli(timestamp))
                        .setPayload(payload)
                        .build();
            }

            case MOTION_SENSOR_EVENT -> {
                MotionSensorProto e = proto.getMotionSensorEvent();
                MotionSensorAvro payload = MotionSensorAvro.newBuilder()
                        .setMotion(e.getMotion())
                        .setLinkQuality(e.getLinkQuality())
                        .setVoltage(e.getVoltage())
                        .build();

                yield SensorEventAvro.newBuilder()
                        .setId(id)
                        .setHubId(hubId)
                        .setTimestamp(Instant.ofEpochMilli(timestamp))
                        .setPayload(payload)
                        .build();
            }

            case SWITCH_SENSOR_EVENT -> {
                SwitchSensorProto e = proto.getSwitchSensorEvent();
                SwitchSensorAvro payload = SwitchSensorAvro.newBuilder()
                        .setState(e.getState())
                        .build();

                yield SensorEventAvro.newBuilder()
                        .setId(id)
                        .setHubId(hubId)
                        .setTimestamp(Instant.ofEpochMilli(timestamp))
                        .setPayload(payload)
                        .build();
            }

            case PAYLOAD_NOT_SET -> throw new IllegalArgumentException(
                    "SensorEventProto payload is not set or unrecognized: " + proto.getPayloadCase()
            );
        };
    }

}
