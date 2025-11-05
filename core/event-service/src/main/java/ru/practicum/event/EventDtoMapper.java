package ru.practicum.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.category.CategoryService;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.event.Location;
import ru.practicum.dto.event.NewEventDto;
import ru.practicum.event.Event;
import ru.practicum.user.UserMapperCustom;
import ru.practicum.user.UserRepository;

@Component
@RequiredArgsConstructor
public class EventDtoMapper {

    private final CategoryService categoryService;
    private final UserRepository userRepository;

    public Event mapToModel(NewEventDto dto, long userId) {
        return Event.builder()
                .initiatorId(userId)
                .annotation(dto.getAnnotation())
                .category(dto.getCategory())
                .description(dto.getDescription())
                .eventDate(dto.getEventDate())
                .locationLat(dto.getLocation().getLat())
                .locationLon(dto.getLocation().getLon())
                .paid(dto.getPaid())
                .participantLimit(dto.getParticipantLimit())
                .requestModeration(dto.getRequestModeration())
                .title(dto.getTitle())
                .build();
    }

    public EventShortDto mapToShortDto(Event event) {
        return EventShortDto.builder()
                .annotation(event.getAnnotation())
                .category(categoryService.getById(event.getCategory()))
                .confirmedRequests((int) event.getConfirmedRequests())
                .eventDate(event.getEventDate())
                .id((int) event.getId())
                .initiator(UserMapperCustom.toUserShortDto(
                        userRepository.findById(event.getInitiatorId())
                                .orElseThrow(() -> new RuntimeException("User not found"))
                ))
                .paid(event.getPaid())
                .title(event.getTitle())
                .views(event.getViews())
                .build();
    }

    public EventFullDto mapToFullDto(Event event) {
        return EventFullDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(categoryService.getById(event.getCategory()))
                .confirmedRequests((int) event.getConfirmedRequests())
                .createdOn(event.getCreatedOn())
                .description(event.getDescription())
                .eventDate(event.getEventDate())
                .initiator(UserMapperCustom.toUserShortDto(
                        userRepository.findById(event.getInitiatorId())
                                .orElseThrow(() -> new RuntimeException("User not found"))
                ))
                .location(Location.builder()
                        .lat(event.getLocationLat())
                        .lon(event.getLocationLon())
                        .build())
                .paid(event.getPaid())
                .participantLimit((int) event.getParticipantLimit())
                .publishedOn(event.getPublishedOn())
                .requestModeration(event.getRequestModeration())
                .state(event.getState())
                .title(event.getTitle())
                .views(event.getViews())
                .build();
    }
}
