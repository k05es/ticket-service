package com.k05es.ticketservice.model;

/**
 * Роли пользователей системы.
 * Определяют, какие действия доступны пользователю
 */
public enum UserRole {

    ADMIN("Администратор"),
    SPECIALIST("Специалист поддержки"),
    CUSTOMER("Клиент");

    // Отображаемое название роли для вывода в консоль
    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}