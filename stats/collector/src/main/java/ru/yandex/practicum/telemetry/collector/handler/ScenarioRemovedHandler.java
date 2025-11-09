package ru.yandex.practicum.telemetry.collector.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.telemetry.collector.mapper.HubEventMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScenarioRemovedHandler implements HubEventHandler {
    private static final String TOPIC = "telemetry.hubs.v1";

    private final Producer<String, SpecificRecordBase> producer;

    @Override
    public void handle(HubEventProto proto) {
        HubEventAvro avroEvent = HubEventMapper.mapToAvro(proto);

        ProducerRecord<String, SpecificRecordBase> record = new ProducerRecord<>(
                TOPIC,
                avroEvent.getHubId().toString(),
                avroEvent
        );

        producer.send(record, (metadata, exception) -> {
            if (exception != null) {
                log.error("Ошибка при отправке события в Kafka: {}", exception.getMessage(), exception);
            } else {
                log.info("Событие успешно отправлено в Kafka. Topic: {}, Partition: {}, Offset: {}",
                        metadata.topic(), metadata.partition(), metadata.offset());
            }
        });
    }

    @Override
    public HubEventProto.PayloadCase getMessageType() {
        return HubEventProto.PayloadCase.SCENARIO_REMOVED;
    }
}
