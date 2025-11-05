package ru.practicum;


import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.dto.comment.CommentDto;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    @Mapping(target = "event", source = "event.id")
    @Mapping(target = "user", source = "user.id")
    CommentDto commentToDto(Comment comment);
}
