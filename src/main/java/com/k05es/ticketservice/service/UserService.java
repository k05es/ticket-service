package com.k05es.ticketservice.service;

import com.k05es.ticketservice.model.User;
import com.k05es.ticketservice.model.UserRole;

import java.util.List;

/**
 * Сервис для работы с пользователями.
 */
public interface UserService {

    // Регистрирует нового пользователя
    User registerUser(User user);

    // Находит пользователя по ID
    User findUserById(Long id);

    // Находит пользователя по логину
    User findUserByUsername(String username);

    // Находит пользователя по email
    User findUserByEmail(String email);

    // Возвращает пользователей с указанной ролью
    List<User> findUsersByRole(UserRole role);

    // Возвращает всех пользователей
    List<User> getAllUsers();

    // Удаляет пользователя по ID
    boolean deleteUser(Long id);
}