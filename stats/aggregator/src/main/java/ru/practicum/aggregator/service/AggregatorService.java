package ru.practicum.aggregator.service;

import ru.practicum.ewm.stats.avro.EventsSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AggregatorService {
    List<EventsSimilarityAvro> updateSimilarity(UserActionAvro userActionAvro);

    List<EventsSimilarityAvro> eventSumMinWeightUpdate(Long eventId, Long userId, Double newWeight,
                                                      Double oldWeight, Instant timestamp);
}
