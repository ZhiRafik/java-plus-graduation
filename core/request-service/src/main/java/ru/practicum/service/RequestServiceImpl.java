package ru.practicum.service;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.client.event.EventClient;
import ru.practicum.client.user.UserAdminClient;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.enums.State;
import ru.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.dto.request.enums.Status;
import ru.practicum.dto.user.UserDto;
import ru.practicum.exception.ConflictPropertyConstraintException;
import ru.practicum.exception.ConflictRelationsConstraintException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.RequestMapper;
import ru.practicum.model.Request;
import ru.practicum.repository.RequestRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {
    private final RequestRepository requestRepository;
    private final RequestMapper requestMapper;
    private final EventClient eventClient;
    private final UserAdminClient userAdminClient;

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        findUser(userId);
        return requestRepository.findByRequesterId(userId).stream()
                .map(requestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());
    }

    @Override
    public ParticipationRequestDto addUserRequest(Long userId, Long eventId) {
        UserDto user = findUser(userId);
        EventFullDto event = findEvent(eventId);
        requestRepository.findByRequesterIdAndEventId(userId, eventId).ifPresent(
                request -> {
                    throw new ConflictPropertyConstraintException("Нельзя добавить повторный запрос");
                }
        );

        if (Objects.equals(event.getInitiator().getId(), user.getId())) {
            throw new ConflictRelationsConstraintException(
                    "Инициатор события не может добавить запрос на участие в своём событии"
            );
        }

        if (!event.getState().equals(State.PUBLISHED)) {
            throw new ConflictRelationsConstraintException("Нельзя участвовать в неопубликованном событии");
        }

        if (event.getConfirmedRequests() == event.getParticipantLimit() && event.getParticipantLimit() != 0) {
            throw new ConflictPropertyConstraintException("Достигнут лимит запросов на участие");
        }

        Status status = Status.PENDING;
        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            status = Status.CONFIRMED;
            addConfirmedRequestToEvent(event);
        }

        Request request = Request.builder()
                .requesterId(user.getId())
                .status(status)
                .eventId(event.getId())
                .created(LocalDateTime.now().withNano(
                        (LocalDateTime.now().getNano() / 1_000_000) * 1_000_000
                        ))
                .build();

        return requestMapper.toParticipationRequestDto(requestRepository.save(request));
    }


    @Override
    public ParticipationRequestDto cancelUserRequest(Long userId, Long requestId) {
        UserDto user = findUser(userId);
        Request request = requestRepository.findById(requestId).orElseThrow(
                () -> new NotFoundException("Запрос с id " + requestId + " не найден")
        );

        request.setStatus(Status.CANCELED);

        return requestMapper.toParticipationRequestDto(requestRepository.save(request));
    }

    @Override
    public EventRequestStatusUpdateResult changeRequestStatus(Long initiatorId, Long eventId, EventRequestStatusUpdateRequest httpRequest) {
        findUser(initiatorId);
        EventFullDto event = findEvent(eventId);
        checkInitiatorEvent(initiatorId, eventId);

        List<Request> requests = requestRepository.findAllByIdIn(httpRequest.getRequestIds());

        if (event.getConfirmedRequests() == event.getParticipantLimit() && event.getParticipantLimit() != 0)
            throw new ConflictPropertyConstraintException("Достигнут лимит запросов на участие");

        List<Long> invalidRequestByEventIds = requests.stream()
                .filter(request -> !Objects.equals(request.getEventId(), eventId))
                .map(Request::getId)
                .toList();

        if (!invalidRequestByEventIds.isEmpty())
            throw new ConflictPropertyConstraintException(
                    "Событие с id " + eventId + " не принадлежит запросам с id " + invalidRequestByEventIds
            );

        List<Long> invalidRequestByStatusIds = requests.stream()
                .filter(request -> request.getStatus() != Status.PENDING)
                .map(Request::getId)
                .toList();
        if (!invalidRequestByStatusIds.isEmpty())
            throw new ConflictPropertyConstraintException("Неверный статус у запросов c id " + invalidRequestByStatusIds);

        if (httpRequest.getStatus().equals(Status.REJECTED)) {
            List<Request> cancelRequests = requests.stream()
                    .peek(request -> request.setStatus(Status.REJECTED))
                    .toList();
            requestRepository.saveAll(cancelRequests);

            List<ParticipationRequestDto> requestDTOs = requests.stream()
                    .map(requestMapper::toParticipationRequestDto)
                    .toList();

            return EventRequestStatusUpdateResult.builder()
                    .confirmedRequests(List.of())
                    .rejectedRequests(requestDTOs)
                    .build();
        }

        long participantLimit = event.getParticipantLimit();
        int confirmedRequests = event.getConfirmedRequests();
        int requestsSize = requests.size();

        if (participantLimit - confirmedRequests < requestsSize)
            throw new ConflictPropertyConstraintException("Достигнут лимит запросов на участие");


        List<Request> confirmedRequestsList = requests.stream()
                .peek(request -> request.setStatus(Status.CONFIRMED))
                .toList();
        requestRepository.saveAll(confirmedRequestsList);
        event.setConfirmedRequests(confirmedRequests + requestsSize);
        eventClient.saveFullEvent(event);

        List<ParticipationRequestDto> confirmedRequestDTOs = confirmedRequestsList.stream()
                .map(requestMapper::toParticipationRequestDto)
                .toList();

        List<ParticipationRequestDto> canceledRequestDTOs = List.of();

        if (participantLimit - confirmedRequests == requestsSize) {
            List<Request> pendingRequests = requestRepository.findByEventIdAndStatus(eventId, Status.PENDING);
            List<Request> cancelRequests = pendingRequests.stream()
                    .peek(request -> request.setStatus(Status.REJECTED))
                    .toList();
            requestRepository.saveAll(cancelRequests);

            canceledRequestDTOs = cancelRequests.stream()
                    .map(requestMapper::toParticipationRequestDto)
                    .toList();
        }

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmedRequestDTOs)
                .rejectedRequests(canceledRequestDTOs)
                .build();

    }



    @Override
    public List<ParticipationRequestDto> getRequestByInitiator(Long userId, Long eventId) {
        checkInitiatorEvent(userId, eventId);
        return requestRepository.findByEventId(eventId).stream()
                .map(requestMapper::toParticipationRequestDto)
                .toList();
    }

    private UserDto findUser(Long userId) {
        return userAdminClient.getUserById(userId);
    }

    private EventFullDto findEvent(Long eventId) {
        return eventClient.getEventById(eventId, "requestService");
    }

    private void addConfirmedRequestToEvent(EventFullDto event) {
        event.setConfirmedRequests(event.getConfirmedRequests() + 1);
        eventClient.saveFullEvent(event);
    }

    private void checkInitiatorEvent(Long initiatorId, Long eventId) {
        if (eventClient.checkInitiatorEvent(initiatorId, eventId) == 0) {
            throw new NotFoundException(
                    "У пользователя с id " + initiatorId + " не найдено событие с id " + eventId
            );
        }
    }
}
