package com.k05es.ticketservice.model;

/**
 * Разрешения администратора.
 * Определяют, какие действия доступны конкретному админу.
 */
public enum Permission {

    MANAGE_USERS("Управление пользователями"),
    MANAGE_SPECIALISTS("Управление специалистами"),
    MANAGE_TICKETS("Управление заявками"),
    VIEW_REPORTS("Просмотр отчётов"),
    DELETE_DATA("Удаление данных");

    // Отображаемое имя разрешения
    private final String displayName;

    Permission(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}