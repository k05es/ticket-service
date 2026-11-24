package com.k05es.ticketservice.service;

import com.k05es.ticketservice.model.User;
import com.k05es.ticketservice.model.UserRole;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Локальная реализация UserService в памяти.
 */
public class LocalUserService implements UserService {

    // ID пользователя -> сам пользователь
    private final Map<Long, User> users = new HashMap<>();

    // Логины в нижнем регистре для проверки уникальности
    private final Set<String> usedUsernames = new HashSet<>();

    // Счётчик ID
    private final AtomicLong idGenerator = new AtomicLong(1);

    // Регистрирует нового пользователя
    @Override
    public User registerUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("Пользователь не может быть null");
        }
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            throw new IllegalArgumentException("Логин не может быть пустым");
        }
        if (usedUsernames.contains(user.getUsername().toLowerCase())) {
            throw new IllegalArgumentException("Логин уже занят: " + user.getUsername());
        }

        long newId = idGenerator.getAndIncrement();
        user.setId(newId);
        users.put(newId, user);
        usedUsernames.add(user.getUsername().toLowerCase());

        return user;
    }

    // Находит пользователя по ID
    @Override
    public User findUserById(Long id) {
        if (id == null) {
            return null;
        }
        return users.get(id);
    }

    // Находит пользователя по логину
    @Override
    public User findUserByUsername(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        for (User user : users.values()) {
            if (user.getUsername() != null
                    && user.getUsername().equalsIgnoreCase(username)) {
                return user;
            }
        }
        return null;
    }

    // Находит пользователя по email
    @Override
    public User findUserByEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        for (User user : users.values()) {
            if (user.getEmail() != null
                    && user.getEmail().equalsIgnoreCase(email)) {
                return user;
            }
        }
        return null;
    }

    // Возвращает пользователей с указанной ролью
    @Override
    public List<User> findUsersByRole(UserRole role) {
        List<User> result = new ArrayList<>();
        if (role == null) {
            return result;
        }
        for (User user : users.values()) {
            if (user.getRole() == role) {
                result.add(user);
            }
        }
        return result;
    }

    // Возвращает всех пользователей
    @Override
    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    // Удаляет пользователя по ID
    @Override
    public boolean deleteUser(Long id) {
        if (id == null) {
            return false;
        }
        User removed = users.remove(id);
        if (removed == null) {
            return false;
        }
        if (removed.getUsername() != null) {
            usedUsernames.remove(removed.getUsername().toLowerCase());
        }
        return true;
    }
}