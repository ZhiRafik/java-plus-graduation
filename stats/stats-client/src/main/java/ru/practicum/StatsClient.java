package ru.practicum;

import com.google.protobuf.Timestamp;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.grpc.stats.action.ActionTypeProto;
import ru.yandex.practicum.grpc.stats.action.UserActionProto;
import ru.yandex.practicum.grpc.stats.dashboard.RecommendationsControllerGrpc;
import ru.yandex.practicum.grpc.stats.collector.UserActionControllerGrpc;
import ru.yandex.practicum.grpc.stats.recommendation.InteractionsCountRequestProto;
import ru.yandex.practicum.grpc.stats.recommendation.RecommendedEventProto;
import ru.yandex.practicum.grpc.stats.recommendation.SimilarEventsRequestProto;
import ru.yandex.practicum.grpc.stats.recommendation.UserPredictionsRequestProto;

import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
public class StatsClient {

    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub collector;

    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub analyzer;

    public Stream<RecommendedEventProto> getSimilarEvents (long eventId, long userId, int maxResults) {
        SimilarEventsRequestProto request = SimilarEventsRequestProto.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setMaxResults(maxResults)
                .build();

        Iterator<RecommendedEventProto> iterator = analyzer.getSimilarEvents(request);
        return asStream(iterator);
    }

    public Stream<RecommendedEventProto> getGetInteractionsCount (List<Long> eventId) {
        InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                .addAllEventId(eventId)
                .build();
        Iterator<RecommendedEventProto> iterator = analyzer.getInteractionsCount(request);

        return asStream(iterator);
    }

    public Stream<RecommendedEventProto> getGetRecommendationsForUser(long userId, int maxResults) {
        UserPredictionsRequestProto request = UserPredictionsRequestProto.newBuilder()
                .setUserId(userId)
                .setMaxResults(maxResults)
                .build();
        Iterator<RecommendedEventProto> iterator = analyzer.getRecommendationsForUser(request);

        return asStream(iterator);
    }

    private Stream<RecommendedEventProto> asStream(Iterator<RecommendedEventProto> iterator) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
    }

    public void sendUserAction (long userId, long eventId, ActionTypeProto actionType) {
        UserActionProto request = UserActionProto.newBuilder()
                .setEventId(eventId)
                .setUserId(userId)
                .setTimestamp(getCurrentTimestamp())
                .setActionType(actionType)
                .build();

        collector.collectUserAction(request);
    }

    private Timestamp getCurrentTimestamp() {
        return Timestamp.newBuilder()
                .setSeconds(Instant.now().getEpochSecond())
                .setNanos(Instant.now().getNano())
                .build();
    }
}
