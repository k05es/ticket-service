package com.k05es.ticketservice.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Заявка клиента.
 * Создаётся клиентом, обрабатывается специалистом.
 * Содержит переписку между клиентом и специалистом.
 */
public class Ticket {

    // Максимальная длина заголовка
    private static final int MAX_TITLE_LENGTH = 50;

    // Максимальная длина описания
    private static final int MAX_DESCRIPTION_LENGTH = 300;

    // Уникальный идентификатор заявки
    private Long id;

    // Краткий заголовок
    private String title;

    // Подробное описание проблемы
    private String description;

    // Текущий статус
    private TicketStatus status;

    // Клиент, создавший заявку
    private CustomerUser customer;

    // Специалист, взявший заявку
    private SpecialistUser specialist;

    // Переписка по заявке
    private List<Message> messages;

    // Дата и время создания
    private LocalDateTime createdAt;

    // Дата и время последнего обновления
    private LocalDateTime updatedAt;

    /**
     * Конструктор по умолчанию.
     * Нужен фреймворкам, которым требуется пустой объект.
     */
    protected Ticket() {
        this.messages = new ArrayList<>();
    }

    /**
     * Конструктор для создания новой заявки.
     * Статус автоматически устанавливается в PENDING.
     */
    public Ticket(Long id, String title, String description, CustomerUser customer) {
        this.id = id;
        setTitle(title);
        setDescription(description);
        this.customer = customer;
        this.status = TicketStatus.PENDING;
        this.specialist = null;
        this.messages = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Геттеры и сеттеры

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    /**
     * Устанавливает заголовок с проверкой длины.
     */
    public void setTitle(String title) {
        if (title != null && title.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException(
                    "Заголовок не может быть длиннее " + MAX_TITLE_LENGTH + " символов");
        }
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Устанавливает описание с проверкой длины.
     */
    public void setDescription(String description) {
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException(
                    "Описание не может быть длиннее " + MAX_DESCRIPTION_LENGTH + " символов");
        }
        this.description = description;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public CustomerUser getCustomer() {
        return customer;
    }

    public void setCustomer(CustomerUser customer) {
        this.customer = customer;
    }

    public SpecialistUser getSpecialist() {
        return specialist;
    }

    public void setSpecialist(SpecialistUser specialist) {
        this.specialist = specialist;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages != null ? messages : new ArrayList<>();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Назначает специалиста и переводит заявку в статус IN_PROGRESS.
     */
    public void assignToSpecialist(SpecialistUser specialist) {
        if (this.status != TicketStatus.PENDING) {
            throw new IllegalStateException("Взять в работу можно только заявку в статусе PENDING");
        }
        this.specialist = specialist;
        this.status = TicketStatus.IN_PROGRESS;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Завершает заявку.
     */
    public void complete() {
        if (this.status != TicketStatus.IN_PROGRESS) {
            throw new IllegalStateException("Завершить можно только заявку в статусе IN_PROGRESS");
        }
        this.status = TicketStatus.COMPLETED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Отменяет заявку.
     */
    public void cancel() {
        if (isClosed()) {
            throw new IllegalStateException("Нельзя отменить закрытую заявку");
        }
        this.status = TicketStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Добавляет сообщение в переписку.
     */
    public void addMessage(User sender, String text) {
        if (isClosed()) {
            throw new IllegalStateException("Нельзя писать в закрытую заявку");
        }
        long messageId = this.messages.size() + 1L;
        Message message = new Message(messageId, text, sender);
        this.messages.add(message);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Проверяет, закрыта ли заявка.
     */
    public boolean isClosed() {
        return this.status == TicketStatus.COMPLETED || this.status == TicketStatus.CANCELLED;
    }

    /**
     * Проверяет, ожидает ли заявка обработки.
     */
    public boolean isPending() {
        return this.status == TicketStatus.PENDING;
    }

    /**
     * Проверяет, находится ли заявка в работе.
     */
    public boolean isInProgress() {
        return this.status == TicketStatus.IN_PROGRESS;
    }

    /**
     * Две заявки равны, если совпадает их ID.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof Ticket otherTicket) {
            return Objects.equals(id, otherTicket.id);
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
     * Краткое строковое представление заявки.
     */
    @Override
    public String toString() {
        return "Ticket{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", status=" + status +
                ", customer=" + (customer != null ? customer.getUsername() : null) +
                ", specialist=" + (specialist != null ? specialist.getUsername() : null) +
                ", messagesCount=" + messages.size() +
                '}';
    }
}