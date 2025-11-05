package ru.practicum;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.comment.DeleteCommentsDto;

@RestController
@RequiredArgsConstructor
public class CommentControllerAdmin {

    private final CommentService commentService;

    @DeleteMapping("/admin/comments")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCommentsByAdmin(@Valid @RequestBody DeleteCommentsDto deleteCommentsDto) {
        commentService.deleteCommentsByAdmin(deleteCommentsDto);
    }
}
