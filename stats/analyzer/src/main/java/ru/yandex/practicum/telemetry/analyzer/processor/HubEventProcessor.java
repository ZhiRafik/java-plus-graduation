package ru.yandex.practicum.telemetry.analyzer.processor;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.telemetry.analyzer.service.HubEventService;

import java.time.Duration;
import java.util.List;


@Slf4j
@Component
public class HubEventProcessor implements Runnable {

    @Autowired
    private HubEventService hubEventService;
    @Autowired
    @Qualifier("kafkaConsumerHubEvent")
    private KafkaConsumer<String, SpecificRecordBase> consumer;
    private final String TOPIC = "telemetry.hubs.v1";

    @Override
    public void run() {
        log.info("Запуск HubEventProcessor с Avro...");

        try {
            consumer.subscribe(List.of(TOPIC));
            log.info("Подписка на Kafka-топик: {}", TOPIC);

            while (true) {
                ConsumerRecords<String, SpecificRecordBase> records = consumer.poll(Duration.ofMillis(100));
                log.info("Получено событий от хаба: {}", records.count());

                for (ConsumerRecord<String, SpecificRecordBase> record : records) {

                    if (!(record.value() instanceof HubEventAvro event)) {
                        log.warn("Неожиданный тип сообщения: {}", record.value().getClass().getSimpleName());
                        continue;
                    }

                    log.info("Запущена обработка события: {}", event.getPayload());
                    hubEventService.processEvent(event);
                }
                consumer.commitAsync();
            }
        } catch (WakeupException e) {
            log.info("Получен сигнал wakeup, завершаем HubEventProcessor...");
        } catch (Exception e) {
            log.error("Неожиданная ошибка в HubEventProcessor", e);
        } finally {
            consumer.close();
            log.info("Kafka consumer закрыт");
        }
    }


    @PreDestroy
    public void shutdown() {
        log.info("Завершение HubEventProcessor...");
        consumer.wakeup();
    }
}