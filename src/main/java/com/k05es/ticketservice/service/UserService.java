package com.k05es.ticketservice.service;

import com.k05es.ticketservice.dto.request.UpdateUserRequest;
import com.k05es.ticketservice.model.User;
import com.k05es.ticketservice.model.UserRole;
import com.k05es.ticketservice.model.UserStatus;
import java.util.List;

/** Сервис для работы с пользователями. */
public interface UserService {
    User registerUser(User user);
    User findUserById(Long id);
    User findUserByUsername(String username);
    User findUserByEmail(String email);
    List<User> findUsersByRole(UserRole role);
    List<User> searchUsers(UserRole role, UserStatus status, String username, String email);
    List<User> getAllUsers();
    User updateUser(Long id, UpdateUserRequest request);
    boolean deleteUser(Long id);
}