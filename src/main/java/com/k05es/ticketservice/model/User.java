package com.k05es.ticketservice.model;

import java.util.Objects;

/**
 * Абстрактный класс пользователя.
 * Базовый для AdminUser, SpecialistUser и CustomerUser.
 * Содержит общие поля и методы для всех типов пользователей.
 */
public abstract class User {

    // Уникальный идентификатор пользователя
    private Long id;

    // Логин для входа в систему
    private String username;

    // Электронная почта
    private String email;

    // Хэш пароля
    private String passHash;

    // Роль пользователя
    private UserRole role;

    // Статус учётной записи
    private UserStatus status;

    // Фамилия
    private String lastName;

    // Имя
    private String firstName;

    // Отчество
    private String middleName;

    /**
     * Конструктор по умолчанию.
     * Нужен фреймворкам, которым требуется пустой объект.
     */
    protected User() {
    }

    /**
     * Конструктор с основными полями.
     * Используется наследниками для инициализации общих данных.
     */
    protected User(Long id, String username, String email, String passHash,
                   UserRole role, UserStatus status,
                   String lastName, String firstName, String middleName) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.passHash = passHash;
        this.role = role;
        this.status = status;
        this.lastName = lastName;
        this.firstName = firstName;
        this.middleName = middleName;
    }

    /**
     * Возвращает отображаемое имя пользователя.
     * Каждый наследник реализует по-своему.
     */
    public abstract String getDisplayName();

    // Геттеры и сеттеры

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassHash() {
        return passHash;
    }

    public void setPassHash(String passHash) {
        this.passHash = passHash;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    /**
     * Собирает ФИО в одну строку.
     * Пропускает отсутствующие части, между частями ставит пробел.
     */
    public String getFullName() {
        StringBuilder sb = new StringBuilder();
        if (lastName != null) {
            sb.append(lastName);
        }
        if (firstName != null) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(firstName);
        }
        if (middleName != null) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(middleName);
        }
        return sb.toString();
    }

    /**
     * Проверяет, активен ли пользователь.
     */
    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }

    /**
     * Проверяет, заблокирован ли пользователь.
     */
    public boolean isBlocked() {
        return this.status == UserStatus.BLOCKED;
    }

    /**
     * Блокирует пользователя.
     */
    public void block() {
        this.status = UserStatus.BLOCKED;
    }

    /**
     * Активирует пользователя.
     */
    public void activate() {
        this.status = UserStatus.ACTIVE;
    }

    /**
     * Два пользователя равны, если совпадает их ID.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof User otherUser) {
            return Objects.equals(id, otherUser.id);
        }
        return false;
    }

    /**
     * Хэш считается по ID.
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * Краткое строковое представление пользователя.
     */
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", fullName='" + getFullName() + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", status=" + status +
                '}';
    }
}