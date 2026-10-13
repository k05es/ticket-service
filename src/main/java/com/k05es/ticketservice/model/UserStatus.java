package com.k05es.ticketservice.model;

/**
 * Статус учётной записи пользователя.
 * Позволяет блокировать и разблокировать пользователей без удаления.
 */
public enum UserStatus {

    ACTIVE("Активен"),
    BLOCKED("Заблокирован");

    // Отображаемое название статуса для вывода в консоль
    private final String displayName;

    UserStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}