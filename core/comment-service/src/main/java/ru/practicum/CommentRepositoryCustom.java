package ru.practicum;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface CommentRepositoryCustom {

    Page<Comment> getComments(String content, Long userId, Long eventId,
                              LocalDateTime rangeStart, LocalDateTime  rangeEnd, Integer from, Integer size,
                              Pageable pageable);
}
