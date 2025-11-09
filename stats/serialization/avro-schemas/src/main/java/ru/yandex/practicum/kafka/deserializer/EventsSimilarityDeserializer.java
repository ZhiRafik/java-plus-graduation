package ru.yandex.practicum.kafka.deserializer;

import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

public class EventsSimilarityDeserializer extends BaseAvroDeserializer<EventsSimilarityAvro> {
    public EventsSimilarityDeserializer() {
        super(EventsSimilarityAvro.getClassSchema());
    }
}
