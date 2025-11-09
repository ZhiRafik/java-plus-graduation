package ru.yandex.practicum.telemetry.aggregator.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.telemetry.aggregator.service.AggregatorService;

import jakarta.annotation.PreDestroy;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregationStarter implements Runnable {

    private final AggregatorService aggregatorService;
    private final KafkaProducer<String, SpecificRecordBase> producer;
    private final KafkaConsumer<String, SpecificRecordBase> consumer;
    @Value("${kafka.topics.sensor-events}")
    private String sensorTopic;
    @Value("${kafka.topics.sensor-snapshot}")
    private String snapshotTopic;

    private volatile boolean running = true;

    @Override
    public void run() {
        try {
            log.info("Подписка на топик " + sensorTopic);
            consumer.subscribe(List.of(sensorTopic));


            while (running) {
                ConsumerRecords<String, SpecificRecordBase> records = consumer.poll(Duration.ofMillis(100));
                log.trace("Получено {} событий", records.count());

                for (ConsumerRecord<String, SpecificRecordBase> record : records) {
                    log.debug("Получено сообщение: offset={}, partition={}, key={}, timestamp={}, value={}",
                            record.offset(), record.partition(), record.key(), record.timestamp(), record.value());

                    if(!(record.value() instanceof SensorEventAvro event)) {
                        log.warn("Неизвестное значение записи: {}", record.value().getClass().getSimpleName());
                        continue;
                    }

                    Optional<SensorsSnapshotAvro> maybeSnapshot = aggregatorService.updateState(event);

                    maybeSnapshot.ifPresent(snapshot -> {
                        try {
                            producer.send(new ProducerRecord<>(snapshotTopic, snapshot));
                            log.info("Снэпшот для хаба {} отправлен {}", snapshot.getHubId(), snapshot);
                        } catch (Exception e) {
                            log.error("Ошибка при отправлении снэпшота в кафку", e);
                        }
                    });
                }

                log.trace("Отправка асинхронного коммита...");
                consumer.commitAsync();
            }
        } catch (WakeupException ignored) {
            log.info("WakeupException: получен сигнал завершения");
        } catch (Exception e) {
            log.error("Ошибка во время обработки событий от датчиков", e);
        } finally {
            try {
                log.info("Завершаем: flush и commitSync");
                producer.flush();
                consumer.commitSync();
            } catch (Exception e) {
                log.error("Ошибка при завершении и commitSync", e);
            } finally {
                log.info("Закрываем продюсер и консьюмер");
                try {
                    producer.close();
                } catch (Exception e) {
                    log.error("Ошибка при закрытии продюсера", e);
                }
                try {
                    consumer.close();
                } catch (Exception e) {
                    log.error("Ошибка при закрытии консьюмера", e);
                }
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Получен сигнал завершения");
        running = false;
        consumer.wakeup(); // чтобы немедленно выйти из poll()
    }
}
