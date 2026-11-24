package com.k05es.ticketservice.model;

/**
 * Предпочтительный способ связи с клиентом.
 * Используется, чтобы специалист знал, как удобнее связаться.
 */

public enum ContactPreference {

    EMAIL("Электронная почта"),
    PHONE("Телефон"),
    SMS("SMS"),
    MESSENGER("Мессенджер");

    // Отображаемое название способа связи для вывода в консоль
    private final String displayName;

    ContactPreference(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}