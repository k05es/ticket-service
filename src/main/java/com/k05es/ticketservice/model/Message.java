package com.k05es.ticketservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Сообщение в переписке по заявке.
 * Отправителем может быть клиент или специалист.
 */
@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 2000)
    private String text;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;
    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    /** Конструктор по умолчанию для JPA. */
    protected Message() { }

    /** Конструктор для создания сообщения. */
    public Message(Long id, String text, User sender) {
        this.id = id;
        this.text = text;
        this.sender = sender;
        this.sentAt = LocalDateTime.now();
    }

    @PrePersist
    private void setSentAtIfMissing() {
        if (sentAt == null) sentAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    public Ticket getTicket() { return ticket; }
    public void setTicket(Ticket ticket) { this.ticket = ticket; }

    @Override
    public String toString() {
        return "Message{" + "id=" + id + ", text='" + text + "'" +
                ", sender=" + (sender != null ? sender.getUsername() : null) +
                ", sentAt=" + sentAt + '}';
    }
}