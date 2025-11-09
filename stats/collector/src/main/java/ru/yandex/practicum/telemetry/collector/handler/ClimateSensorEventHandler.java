package ru.yandex.practicum.telemetry.collector.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.telemetry.collector.mapper.SensorEventMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClimateSensorEventHandler implements SensorEventHandler {

    private static final String TOPIC = "telemetry.sensors.v1";

    private final Producer<String, SpecificRecordBase> producer;

    @Override
    public void handle(SensorEventProto proto) {
        SensorEventAvro avroEvent = SensorEventMapper.mapToAvro(proto);

        ProducerRecord<String, SpecificRecordBase> record = new ProducerRecord<>(
                TOPIC,
                avroEvent.getId().toString(),
                avroEvent
        );

        producer.send(record, (metadata, exception) -> {
            if (exception != null) {
                log.error("Ошибка при отправке sensor-события в Kafka: {}", exception.getMessage(), exception);
            } else {
                log.info("Sensor-событие отправлено в Kafka: topic={}, partition={}, offset={}",
                        metadata.topic(), metadata.partition(), metadata.offset());
            }
        });
    }

    @Override
    public SensorEventProto.PayloadCase getMessageType() {
        return SensorEventProto.PayloadCase.CLIMATE_SENSOR_EVENT;
    }

}
