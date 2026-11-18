package com.k05es.ticketservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Абстрактный класс пользователя.
 * Базовый для AdminUser, SpecialistUser и CustomerUser.
 * Содержит общие поля и методы для всех типов пользователей.
 */
@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_username", columnNames = "username"),
        @UniqueConstraint(name = "uk_users_email", columnNames = "email")
})
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "user_type", discriminatorType = DiscriminatorType.STRING)
public abstract class User {

    // Уникальный идентификатор пользователя
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Логин для входа в систему
    @Column(nullable = false, length = 50)
    private String username;

    // Электронная почта
    @Column(nullable = false, length = 120)
    private String email;

    // Хэш пароля
    @Column(name = "pass_hash", nullable = false, length = 255)
    private String passHash;

    // Роль пользователя
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    // Статус учётной записи
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    // Фамилия
    @Column(name = "last_name", length = 80)
    private String lastName;

    // Имя
    @Column(name = "first_name", length = 80)
    private String firstName;

    // Отчество
    @Column(name = "middle_name", length = 80)
    private String middleName;

    /** Конструктор по умолчанию для JPA. */
    protected User() {
    }

    /** Конструктор с основными полями. */
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

    /** Возвращает отображаемое имя пользователя. */
    public abstract String getDisplayName();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassHash() { return passHash; }
    public void setPassHash(String passHash) { this.passHash = passHash; }
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }

    /** Собирает ФИО в одну строку. */
    public String getFullName() {
        StringBuilder sb = new StringBuilder();
        if (lastName != null && !lastName.isBlank()) sb.append(lastName);
        if (firstName != null && !firstName.isBlank()) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(firstName);
        }
        if (middleName != null && !middleName.isBlank()) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(middleName);
        }
        return sb.toString();
    }

    public boolean isActive() { return this.status == UserStatus.ACTIVE; }
    public boolean isBlocked() { return this.status == UserStatus.BLOCKED; }
    public void block() { this.status = UserStatus.BLOCKED; }
    public void activate() { this.status = UserStatus.ACTIVE; }

    // Равенство сущностей определяется постоянным идентификатором.
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof User otherUser)) return false;
        return id != null && id.equals(otherUser.id);
    }

    @Override
    public int hashCode() {
        return User.class.hashCode();
    }

    @Override
    public String toString() {
        return "User{" + "id=" + id + ", username='" + username + "'" +
                ", fullName='" + getFullName() + "'" + ", role=" + role +
                ", status=" + status + '}';
    }
}