package ru.practicum.aggregator.service;

import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AggregatorService {
    List<EventSimilarityAvro> updateSimilarity(UserActionAvro userActionAvro);

    List<EventSimilarityAvro> eventSumMinWeightUpdate(Long eventId, Long userId, Double newWeight,
                                                      Double oldWeight, Instant timestamp);
}
