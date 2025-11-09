package ru.yandex.practicum.telemetry.collector.controller;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.yandex.practicum.grpc.telemetry.collector.CollectorControllerGrpc;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.telemetry.collector.handler.HubEventHandler;
import ru.yandex.practicum.telemetry.collector.handler.SensorEventHandler;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class EventController extends CollectorControllerGrpc.CollectorControllerImplBase {

    private final Set<SensorEventHandler> sensorEventHandlersInit;
    private final Set<HubEventHandler> hubEventHandlersInit;
    private Map<SensorEventProto.PayloadCase, SensorEventHandler> sensorEventHandlers;
    private Map<HubEventProto.PayloadCase, HubEventHandler> hubEventHandlers;

    @PostConstruct
    void init() {
        log.info("Инициализация EventController...");
        sensorEventHandlers = sensorEventHandlersInit.stream()
                .collect(Collectors.toMap(SensorEventHandler::getMessageType, Function.identity()));
        hubEventHandlers = hubEventHandlersInit.stream()
                .collect(Collectors.toMap(HubEventHandler::getMessageType, Function.identity()));

        log.info("Sensor handlers: {}", sensorEventHandlers.keySet());
        log.info("Hub handlers: {}", hubEventHandlers.keySet());
    }

    @Override
    public void collectSensorEvent(SensorEventProto request, StreamObserver<Empty> responseObserver) {
        log.info("Получено событие от сенсора: {}", request);
        try {
            SensorEventProto.PayloadCase payloadType = request.getPayloadCase();
            log.info("Тип payload: {}", payloadType);

            if (sensorEventHandlers.containsKey(payloadType)) {
                log.info("Передаём обработку в handler: {}", sensorEventHandlers.get(payloadType).getClass().getSimpleName());
                sensorEventHandlers.get(payloadType).handle(request);
            } else {
                log.warn("Не найден обработчик для события сенсора: {}", payloadType);
                throw new IllegalArgumentException("Не могу найти обработчик для события сенсора " + payloadType);
            }

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
            log.info("Событие сенсора успешно обработано.");
        } catch (Exception e) {
            log.error("Ошибка при обработке события сенсора", e);
            responseObserver.onError(new StatusRuntimeException(Status.fromThrowable(e)));
        }
    }

    @Override
    public void collectHubEvent(HubEventProto request, StreamObserver<Empty> responseObserver) {
        log.info("Получено событие от хаба: {}", request);
        try {
            HubEventProto.PayloadCase payloadType = request.getPayloadCase();
            log.info("Тип payload: {}", payloadType);

            if (hubEventHandlers.containsKey(payloadType)) {
                log.info("Передаём обработку в handler: {}", hubEventHandlers.get(payloadType).getClass().getSimpleName());
                hubEventHandlers.get(payloadType).handle(request);
            } else {
                log.warn("Не найден обработчик для события хаба: {}", payloadType);
                throw new IllegalArgumentException("Не могу найти обработчик для события хаба " + payloadType);
            }

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
            log.info("Событие хаба успешно обработано.");
        } catch (Exception e) {
            log.error("Ошибка при обработке события хаба", e);
            responseObserver.onError(new StatusRuntimeException(Status.fromThrowable(e)));
        }
    }
}
