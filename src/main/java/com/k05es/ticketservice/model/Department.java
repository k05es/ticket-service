package com.k05es.ticketservice.model;

/**
 * Отдел, в котором работает администратор.
 */
public enum Department {

    GENERAL_SUPPORT("Общая поддержка"),
    TECHNICAL_SUPPORT("Техническая поддержка"),
    SHOP_SUPPORT("Поддержка магазина");

    // Отображаемое имя отдела
    private final String displayName;

    Department(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}