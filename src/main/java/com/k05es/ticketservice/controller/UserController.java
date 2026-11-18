package com.k05es.ticketservice.controller;

import com.k05es.ticketservice.dto.request.CreateUserRequest;
import com.k05es.ticketservice.dto.request.UpdateUserRequest;
import com.k05es.ticketservice.dto.response.UserResponse;
import com.k05es.ticketservice.exception.ResourceNotFoundException;
import com.k05es.ticketservice.mapper.UserMapper;
import com.k05es.ticketservice.model.User;
import com.k05es.ticketservice.model.UserRole;
import com.k05es.ticketservice.model.UserStatus;
import com.k05es.ticketservice.service.UserService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
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

/** REST-контроллер для CRUD-операций с пользователями. */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    public UserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        User created = userService.registerUser(userMapper.toEntity(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(userMapper.toResponse(created));
    }

    @GetMapping
    public List<UserResponse> getUsers(@RequestParam(required = false) UserRole role,
                                       @RequestParam(required = false) UserStatus status,
                                       @RequestParam(required = false) String username,
                                       @RequestParam(required = false) String email) {
        return userService.searchUsers(role, status, username, email).stream()
                .map(userMapper::toResponse).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable Long id) {
        return userMapper.toResponse(userService.findUserById(id));
    }

    @PutMapping("/{id}")
    public UserResponse updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return userMapper.toResponse(userService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (!userService.deleteUser(id)) throw new ResourceNotFoundException("Пользователь с ID " + id + " не найден");
        return ResponseEntity.noContent().build();
    }
}