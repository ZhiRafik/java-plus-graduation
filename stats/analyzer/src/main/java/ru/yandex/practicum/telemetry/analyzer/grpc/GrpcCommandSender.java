package ru.yandex.practicum.telemetry.analyzer.grpc;

import com.google.protobuf.Timestamp;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionRequestProto;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc.HubRouterControllerBlockingStub;

import java.time.Instant;

@Slf4j
@Service
public class GrpcCommandSender {

    private final HubRouterControllerBlockingStub hubRouterClient;

    public GrpcCommandSender(@GrpcClient("hub-router")
                                  HubRouterControllerBlockingStub hubRouterClient) {
        this.hubRouterClient = hubRouterClient;
    }


    public void sendDeviceActions(List<DeviceActionRequestProto> actions) {
        if (actions.isEmpty()) {
            log.debug("Получен на обработку пусток список действий");
            return;
        }

        for (DeviceActionRequestProto action : actions) {
            log.debug("Отправляю действие: {}", action);
            hubRouterClient.handleDeviceAction(action);
        }
    }

    private Timestamp currentTimestamp() {
        Instant now = Instant.now();
        return Timestamp.newBuilder()
                .setSeconds(now.getEpochSecond())
                .setNanos(now.getNano())
                .build();
    }
}