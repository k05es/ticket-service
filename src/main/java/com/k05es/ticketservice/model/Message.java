package com.k05es.ticketservice.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Сообщение в переписке по заявке.
 * Отправителем может быть клиент или специалист.
 */
public class Message {

    // Уникальный идентификатор сообщения
    private Long id;

    // Текст сообщения
    private String text;

    // Отправитель
    private User sender;

    // Дата и время отправки
    private LocalDateTime sentAt;

    /**
     * Конструктор по умолчанию.
     * Нужен фреймворкам, которым требуется пустой объект.
     */
    protected Message() {
    }

    /**
     * Конструктор со всеми полями.
     * Дата отправки устанавливается автоматически.
     */
    public Message(Long id, String text, User sender) {
        this.id = id;
        this.text = text;
        this.sender = sender;
        this.sentAt = LocalDateTime.now();
    }

    // Геттеры и сеттеры

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    /**
     * Два сообщения равны, если совпадает их ID.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof Message otherMessage) {
            return Objects.equals(id, otherMessage.id);
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
     * Краткое строковое представление сообщения.
     */
    @Override
    public String toString() {
        return "Message{" +
                "id=" + id +
                ", text='" + text + '\'' +
                ", sender=" + (sender != null ? sender.getUsername() : null) +
                ", sentAt=" + sentAt +
                '}';
    }
}