package ru.practicum.client.user;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.client.MyFeignClientFallback;
import ru.practicum.dto.user.UserCreateDto;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserShortDto;

import java.util.List;

@FeignClient(name = "user-service", path = "/admin/users", fallback = MyFeignClientFallback.class)
public interface UserAdminClient {

    @PostMapping
    UserDto createUser(@RequestBody UserCreateDto userCreateDto);

    @GetMapping
    List<UserDto> getUsers(@RequestParam(required = false) List<Long> ids,
                           @RequestParam(defaultValue = "0") Integer from,
                           @RequestParam(defaultValue = "10") Integer size);

    @DeleteMapping("/{userId}")
    void deleteUserById(@PathVariable Long userId);

    @GetMapping
    UserDto getUserById(@RequestParam Long id);

    @GetMapping("/short")
    UserShortDto getUserShortById(@RequestParam Long id);
}