package ru.yandex.practicum.telemetry.analyzer.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.DeviceAddedEventAvro;
import ru.yandex.practicum.telemetry.analyzer.model.Sensor;
import ru.yandex.practicum.telemetry.analyzer.repository.SensorRepository;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceAddedEventHandler implements HubEventHandler<DeviceAddedEventAvro> {

    private final SensorRepository sensorRepository;

    @Override
    public void handle(DeviceAddedEventAvro payload, String hubId, Instant timestamp) {
        log.info("Добавление сенсора: id={}, hubId={}, timestamp={}", payload.getId(), hubId, timestamp);
        Sensor sensor = Sensor.builder()
                .id(payload.getId())
                .hubId(hubId)
                .build();
        sensorRepository.save(sensor);
        log.debug("Сенсор {} сохранён в базу", payload.getId());
    }

    @Override
    public Class<DeviceAddedEventAvro> getMessageType() {
        return DeviceAddedEventAvro.class;
    }
}
