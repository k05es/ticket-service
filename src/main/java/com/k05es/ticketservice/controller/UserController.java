package com.k05es.ticketservice.controller;

import com.k05es.ticketservice.dto.request.CreateUserRequest;
import com.k05es.ticketservice.dto.request.UpdateUserRequest;
import com.k05es.ticketservice.dto.response.UserResponse;
import com.k05es.ticketservice.exception.UserNotFoundException;
import com.k05es.ticketservice.mapper.UserMapper;
import com.k05es.ticketservice.model.User;
import com.k05es.ticketservice.model.UserRole;
import com.k05es.ticketservice.model.UserStatus;
import com.k05es.ticketservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** REST-контроллер для CRUD-операций с пользователями (управление ролями доступно ADMIN). */
@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Управление пользователями и ролями (ADMIN)")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    public UserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Создать пользователя (только ADMIN)")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        User created = userService.registerUser(userMapper.toEntity(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(userMapper.toResponse(created));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Получить список пользователей с фильтрацией (только ADMIN)")
    public List<UserResponse> getUsers(@RequestParam(required = false) UserRole role,
                                       @RequestParam(required = false) UserStatus status,
                                       @RequestParam(required = false) String username,
                                       @RequestParam(required = false) String email) {
        List<User> users = userService.searchUsers(role, status, username, email);
        List<UserResponse> responses = new ArrayList<UserResponse>();
        for (User user : users) {
            responses.add(userMapper.toResponse(user));
        }
        return responses;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить пользователя по ID (аутентифицированные пользователи)")
    public UserResponse getUser(@PathVariable Long id) {
        return userMapper.toResponse(userService.findUserById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Обновить профиль пользователя (только ADMIN)")
    public UserResponse updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return userMapper.toResponse(userService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Удалить пользователя по ID (только ADMIN)")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (!userService.deleteUser(id)) {
            throw UserNotFoundException.byId(id);
        }
        return ResponseEntity.noContent().build();
    }
}
