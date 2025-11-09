package ru.practicum.collector.kafka;

import ru.yandex.practicum.grpc.stats.action.UserActionProto;

public interface KafkaService {
    void kafkaUserAction(UserActionProto userActionProto);
}
