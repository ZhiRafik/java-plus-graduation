package ru.practicum.analyzer.service;

import ru.practicum.analyzer.model.EventSimilarity;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

public interface EventSimilarityService {
    EventSimilarity processEventSimilarity(EventSimilarityAvro eventSimilarityAvro);
}
