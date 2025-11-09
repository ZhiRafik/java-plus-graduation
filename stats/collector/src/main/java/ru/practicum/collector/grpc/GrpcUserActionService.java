package ru.practicum.collector.grpc;

import ru.yandex.practicum.grpc.stats.action.UserActionProto;

public interface GrpcUserActionService {
    void processUserAction(UserActionProto userActionProto);
}

