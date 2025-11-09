package ru.yandex.practicum.telemetry.analyzer.handler;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class HubEventDispatcher {
    private final Map<Class<?>, HubEventHandler<?>> handlers = new HashMap<>();

    public HubEventDispatcher(List<HubEventHandler<?>> handlerList) {
        for (HubEventHandler<?> handler : handlerList) {
            handlers.put(handler.getMessageType(), handler);
            log.info("Хендлер зарегистрирован: {} → {}", handler.getMessageType().getSimpleName(), handler.getClass().getSimpleName());
        }
    }


    public void dispatch(HubEventAvro event) {

        SpecificRecordBase payload = (SpecificRecordBase) event.getPayload();
        HubEventHandler<?> handler = handlers.get(payload.getClass());

        if (handler == null) {
            log.error("Хендлер не найден для типа payload: {}", payload.getClass().getSimpleName());
            throw new IllegalArgumentException("Хендлер не найден для типа payload: " + payload.getClass());
        }

        @SuppressWarnings("unchecked")
        HubEventHandler<SpecificRecordBase> typedHandler = (HubEventHandler<SpecificRecordBase>) handler;
        typedHandler.handle(payload, event.getHubId(), event.getTimestamp());
        log.info("Событие обработано: hubId={}, payloadType={}", event.getHubId(), payload.getClass().getSimpleName());
    }
}
