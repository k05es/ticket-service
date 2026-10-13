package com.k05es.ticketservice.model;

/**
 * Статусы заявки.
 * Описывают жизненный цикл заявки от создания до закрытия.
 */
public enum TicketStatus {

    PENDING("Ожидает"),
    IN_PROGRESS("В работе"),
    COMPLETED("Завершена"),
    CANCELLED("Отменена");

    // Отображаемое название статуса для вывода в консоль
    private final String displayName;

    TicketStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}