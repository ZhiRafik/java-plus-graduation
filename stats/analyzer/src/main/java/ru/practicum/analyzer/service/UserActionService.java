package ru.practicum.analyzer.service;

import ru.practicum.analyzer.model.UserAction;

public interface UserActionService {
    UserAction processUserAction(UserActionAvro userAction);

    UserAction findUserActionByEventIdAndUserId(Long eventId,  Long userId);
}
