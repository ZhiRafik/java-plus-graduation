package ru.yandex.practicum.telemetry.analyzer.service;

import ru.yandex.practicum.grpc.telemetry.event.DeviceActionRequestProto;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.util.List;

public interface ScenarioService {
    List<DeviceActionRequestProto> processSnapshot(SensorsSnapshotAvro snapshot);
}