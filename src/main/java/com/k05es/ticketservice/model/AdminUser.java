package com.k05es.ticketservice.model;

import java.util.HashSet;
import java.util.Set;

/**
 * Администратор системы.
 * Управляет пользователями и специалистами.
 * Роль — ADMIN.
 */
public class AdminUser extends User {

    // Набор разрешений
    private Set<Permission> permissions;

    // Отдел
    private Department department;

    // Идентификаторы закреплённых специалистов
    private Set<Long> responsibleForSpecialists;

    /**
     * Конструктор по умолчанию.
     * Нужен фреймворкам, которым требуется пустой объект.
     */
    protected AdminUser() {
        super();
        this.permissions = new HashSet<>();
        this.responsibleForSpecialists = new HashSet<>();
    }

    /**
     * Конструктор со всеми полями.
     */
    public AdminUser(Long id, String username, String email, String passHash,
                     UserStatus status,
                     String lastName, String firstName, String middleName,
                     Set<Permission> permissions,
                     Department department,
                     Set<Long> responsibleForSpecialists) {
        super(id, username, email, passHash, UserRole.ADMIN, status,
                lastName, firstName, middleName);
        this.permissions = permissions != null ? permissions : new HashSet<>();
        this.department = department;
        this.responsibleForSpecialists = responsibleForSpecialists != null
                ? responsibleForSpecialists
                : new HashSet<>();
    }

    // Геттеры и сеттеры

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<Permission> permissions) {
        this.permissions = permissions != null ? permissions : new HashSet<>();
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Set<Long> getResponsibleForSpecialists() {
        return responsibleForSpecialists;
    }

    public void setResponsibleForSpecialists(Set<Long> responsibleForSpecialists) {
        this.responsibleForSpecialists = responsibleForSpecialists != null
                ? responsibleForSpecialists
                : new HashSet<>();
    }

    /**
     * Добавляет разрешение администратору.
     */
    public void addPermission(Permission permission) {
        if (permission != null) {
            this.permissions.add(permission);
        }
    }

    /**
     * Убирает разрешение у администратора.
     */
    public void removePermission(Permission permission) {
        this.permissions.remove(permission);
    }

    /**
     * Проверяет, есть ли у администратора указанное разрешение.
     */
    public boolean hasPermission(Permission permission) {
        return this.permissions.contains(permission);
    }

    /**
     * Закрепляет специалиста за администратором.
     */
    public void addResponsibleSpecialist(Long specialistId) {
        if (specialistId != null) {
            this.responsibleForSpecialists.add(specialistId);
        }
    }

    /**
     * Снимает специалиста с закрепления.
     */
    public void removeResponsibleSpecialist(Long specialistId) {
        this.responsibleForSpecialists.remove(specialistId);
    }

    /**
     * Проверяет, закреплён ли специалист за администратором.
     */
    public boolean isResponsibleFor(Long specialistId) {
        return this.responsibleForSpecialists.contains(specialistId);
    }

    /**
     * Возвращает отображаемое имя администратора.
     */
    @Override
    public String getDisplayName() {
        String fullName = getFullName();
        if (fullName != null && !fullName.isBlank()) {
            return "Администратор " + fullName + " (" + getUsername() + ")";
        }
        return "Администратор (" + getUsername() + ")";
    }

    /**
     * Краткое строковое представление администратора.
     */
    @Override
    public String toString() {
        return "AdminUser{" +
                "id=" + getId() +
                ", username='" + getUsername() + '\'' +
                ", fullName='" + getFullName() + '\'' +
                ", email='" + getEmail() + '\'' +
                ", status=" + getStatus() +
                ", department=" + department +
                ", permissions=" + permissions +
                ", responsibleForSpecialists=" + responsibleForSpecialists +
                '}';
    }
}