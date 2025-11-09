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
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionRequestProto;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.telemetry.analyzer.grpc.GrpcCommandSender;
import ru.yandex.practicum.telemetry.analyzer.service.ScenarioService;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
public class SnapshotProcessor implements Runnable {

    @Autowired
    @Qualifier("kafkaConsumerSnapshot")
    private KafkaConsumer<String, SpecificRecordBase> consumer;

    @Autowired
    private GrpcCommandSender grpcSender;
    @Autowired
    private ScenarioService scenarioService;
    private final String TOPIC = "telemetry.snapshots.v1";

    @Override
    public void run() {
        log.info("Запуск SnapshotProcessor...");
        try {
            log.info("Анализатор в SnapshotProcessor подписался на топик: {}", TOPIC);
            consumer.subscribe(List.of(TOPIC));
            while (true) {
                ConsumerRecords<String, SpecificRecordBase> records = consumer.poll(Duration.ofMillis(100));
                log.info("Получено снапшотов: {}", records.count());

                for (ConsumerRecord<String, SpecificRecordBase> record : records) {
                    if(!(record.value() instanceof SensorsSnapshotAvro sensorsSnapshotAvro)) {
                        log.warn("Неожиданный тип данных: {}", record.value().getClass().getSimpleName());
                        continue;
                    }
                    log.info("Обрабатываю snapshot");
                    List<DeviceActionRequestProto> actionRequests = scenarioService.processSnapshot(sensorsSnapshotAvro);

                    grpcSender.sendDeviceActions(actionRequests);
                    log.info("Анализатор отправил запросы: {} ", actionRequests);
                }
                consumer.commitSync();
            }
        } catch (WakeupException e) {
            log.info("Получен сигнал wakeup, завершаем SnapshotProcessor...");
        } catch (Exception e) {
            log.error("Неожиданная ошибка в SnapshotProcessor", e);
        } finally {
            consumer.close();
            log.info("Kafka consumer закрыт");
        }
    }

    @PreDestroy
    public void shutdown() {
        consumer.wakeup();
    }
}
