package ru.yandex.practicum.telemetry.analyzer.handler;

import org.apache.avro.specific.SpecificRecordBase;

import java.time.Instant;

public interface HubEventHandler <T extends SpecificRecordBase> {

    Class<T> getMessageType();

    void handle(T payload, String hubId, Instant timestamp);
}

