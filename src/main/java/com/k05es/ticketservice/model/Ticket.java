package com.k05es.ticketservice.model;

import com.k05es.ticketservice.exception.InvalidDomainValueException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.persistence.Index;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Заявка клиента.
 * Создаётся клиентом, обрабатывается специалистом.
 * Содержит переписку между клиентом и специалистом.
 */
@Entity
@Table(name = "tickets", indexes = {
        @Index(name = "idx_tickets_status", columnList = "status"),
        @Index(name = "idx_tickets_customer", columnList = "customer_id"),
        @Index(name = "idx_tickets_specialist", columnList = "specialist_id")
})
public class Ticket {

    private static final int MAX_TITLE_LENGTH = 50;
    private static final int MAX_DESCRIPTION_LENGTH = 300;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = MAX_TITLE_LENGTH)
    private String title;
    @Column(nullable = false, length = MAX_DESCRIPTION_LENGTH)
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketStatus status;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerUser customer;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specialist_id")
    private SpecialistUser specialist;
    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sentAt ASC")
    private List<Message> messages = new ArrayList<>();
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    @Version
    @Column(nullable = false)
    private long version;

    /** Конструктор по умолчанию для JPA. */
    protected Ticket() { this.messages = new ArrayList<>(); }

    /** Конструктор для создания новой заявки. */
    public Ticket(Long id, String title, String description, CustomerUser customer) {
        this.id = id;
        setTitle(title);
        setDescription(description);
        this.customer = customer;
        this.status = TicketStatus.PENDING;
        this.messages = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    private void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (status == null) status = TicketStatus.PENDING;
    }

    @PreUpdate
    private void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) {
        if (title == null || title.isBlank()) throw new InvalidDomainValueException("Заголовок обязателен");
        if (title.length() > MAX_TITLE_LENGTH) throw new InvalidDomainValueException("Заголовок не может быть длиннее " + MAX_TITLE_LENGTH + " символов");
        this.title = title;
    }
    public String getDescription() { return description; }
    public void setDescription(String description) {
        if (description == null || description.isBlank()) throw new InvalidDomainValueException("Описание обязательно");
        if (description.length() > MAX_DESCRIPTION_LENGTH) throw new InvalidDomainValueException("Описание не может быть длиннее " + MAX_DESCRIPTION_LENGTH + " символов");
        this.description = description;
    }
    public TicketStatus getStatus() { return status; }
    public void setStatus(TicketStatus status) { this.status = status; }
    public CustomerUser getCustomer() { return customer; }
    public void setCustomer(CustomerUser customer) { this.customer = customer; }
    public SpecialistUser getSpecialist() { return specialist; }
    public void setSpecialist(SpecialistUser specialist) { this.specialist = specialist; }
    public List<Message> getMessages() { return messages; }
    public void setMessages(List<Message> messages) {
        this.messages = messages != null ? messages : new ArrayList<>();
        for (Message message : this.messages) message.setTicket(this);
    }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public long getVersion() { return version; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public void assignToSpecialist(SpecialistUser specialist) {
        if (this.status != TicketStatus.PENDING) throw new IllegalStateException("Взять в работу можно только заявку в статусе PENDING");
        if (specialist == null || !specialist.canTakeMoreTickets()) throw new IllegalArgumentException("Специалист недоступен");
        this.specialist = specialist;
        this.status = TicketStatus.IN_PROGRESS;
        this.updatedAt = LocalDateTime.now();
    }

    public void complete() {
        if (this.status != TicketStatus.IN_PROGRESS) throw new IllegalStateException("Завершить можно только заявку в статусе IN_PROGRESS");
        this.status = TicketStatus.COMPLETED;
        this.updatedAt = LocalDateTime.now();
    }

    public void cancel() {
        if (isClosed()) throw new IllegalStateException("Нельзя отменить закрытую заявку");
        this.status = TicketStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    public void addMessage(User sender, String text) {
        if (isClosed()) throw new IllegalStateException("Нельзя писать в закрытую заявку");
        if (sender == null || text == null || text.isBlank()) throw new IllegalArgumentException("Отправитель и текст сообщения обязательны");
        Message message = new Message(null, text, sender);
        message.setTicket(this);
        this.messages.add(message);
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isClosed() { return status == TicketStatus.COMPLETED || status == TicketStatus.CANCELLED; }
    public boolean isPending() { return status == TicketStatus.PENDING; }
    public boolean isInProgress() { return status == TicketStatus.IN_PROGRESS; }

    @Override
    public String toString() {
        return "Ticket{" + "id=" + id + ", title='" + title + "'" +
                ", status=" + status + ", customer=" + (customer != null ? customer.getUsername() : null) +
                ", specialist=" + (specialist != null ? specialist.getUsername() : null) +
                ", messagesCount=" + (messages != null ? messages.size() : 0) + '}';
    }
}