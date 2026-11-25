package com.k05es.ticketservice.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Column;
import java.util.HashSet;
import java.util.Set;

/**
 * Администратор системы.
 * Управляет пользователями и специалистами.
 * Роль — ADMIN.
 */
@Entity
@DiscriminatorValue("ADMIN")
public class AdminUser extends User {

    // Набор разрешений
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "admin_permissions", joinColumns = @JoinColumn(name = "admin_id"))
    @Column(name = "permission", nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    private Set<Permission> permissions = new HashSet<>();

    // Отдел
    @Enumerated(EnumType.STRING)
    @Column(name = "admin_department", length = 30)
    private Department department;

    // Идентификаторы закреплённых специалистов
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "admin_specialists", joinColumns = @JoinColumn(name = "admin_id"))
    @Column(name = "specialist_id", nullable = false)
    private Set<Long> responsibleForSpecialists = new HashSet<>();

    /** Конструктор по умолчанию для JPA. */
    protected AdminUser() { super(); }

    /** Конструктор со всеми полями. */
    public AdminUser(Long id, String username, String email, String passHash,
                     UserStatus status, String lastName, String firstName, String middleName,
                     Set<Permission> permissions, Department department,
                     Set<Long> responsibleForSpecialists) {
        super(id, username, email, passHash, UserRole.ADMIN, status, lastName, firstName, middleName);
        this.permissions = permissions != null ? permissions : new HashSet<>();
        this.department = department;
        this.responsibleForSpecialists = responsibleForSpecialists != null
                ? responsibleForSpecialists : new HashSet<>();
    }

    public Set<Permission> getPermissions() { return permissions; }
    public void setPermissions(Set<Permission> permissions) {
        this.permissions = permissions != null ? permissions : new HashSet<>();
    }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public Set<Long> getResponsibleForSpecialists() { return responsibleForSpecialists; }
    public void setResponsibleForSpecialists(Set<Long> values) {
        this.responsibleForSpecialists = values != null ? values : new HashSet<>();
    }

    public void addPermission(Permission permission) { if (permission != null) permissions.add(permission); }
    public void removePermission(Permission permission) { permissions.remove(permission); }
    public boolean hasPermission(Permission permission) { return permissions.contains(permission); }
    public void addResponsibleSpecialist(Long specialistId) {
        if (specialistId != null) responsibleForSpecialists.add(specialistId);
    }
    public void removeResponsibleSpecialist(Long specialistId) { responsibleForSpecialists.remove(specialistId); }
    public boolean isResponsibleFor(Long specialistId) { return responsibleForSpecialists.contains(specialistId); }

    @Override
    public String getDisplayName() {
        String fullName = getFullName();
        return fullName != null && !fullName.isBlank()
                ? "Администратор " + fullName + " (" + getUsername() + ")"
                : "Администратор (" + getUsername() + ")";
    }
}