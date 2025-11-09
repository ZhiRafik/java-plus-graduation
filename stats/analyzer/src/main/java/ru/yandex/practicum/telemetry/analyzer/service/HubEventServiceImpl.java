package ru.yandex.practicum.telemetry.analyzer.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.telemetry.analyzer.handler.HubEventDispatcher;

@Slf4j
@Service
@RequiredArgsConstructor
public class HubEventServiceImpl implements HubEventService {

    private final HubEventDispatcher hubEventDispatcher;

    @Transactional
    public void processEvent(HubEventAvro hubEventavro) {
        log.info("Запущена обработка события от хаба " + hubEventavro.getHubId() +
                " с типом " + hubEventavro.getClass());
        hubEventDispatcher.dispatch(hubEventavro);
    }

}
