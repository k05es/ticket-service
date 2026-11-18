package com.k05es.ticketservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * Специалист системы.
 * Обрабатывает заявки клиентов.
 * Роль — SPECIALIST.
 */
@Entity
@DiscriminatorValue("SPECIALIST")
public class SpecialistUser extends User {

    @Column(name = "average_rating", nullable = false)
    private double averageRating;
    @Column(name = "completed_ticket_count", nullable = false)
    private int completedTicketCount;
    @Column(name = "completed_today_count", nullable = false)
    private int completedTodayCount;
    @Column(name = "active_ticket_count", nullable = false)
    private int activeTicketCount;
    @Column(nullable = false)
    private boolean available;
    @Enumerated(EnumType.STRING)
    @Column(name = "specialist_department", length = 30)
    private Department department;

    /** Конструктор по умолчанию для JPA. */
    protected SpecialistUser() { super(); this.available = true; }

    public SpecialistUser(Long id, String username, String email, String passHash,
                          UserStatus status, String lastName, String firstName, String middleName,
                          double averageRating, int completedTicketCount, int completedTodayCount,
                          int activeTicketCount, boolean available, Department department) {
        super(id, username, email, passHash, UserRole.SPECIALIST, status,
                lastName, firstName, middleName);
        this.averageRating = averageRating;
        this.completedTicketCount = completedTicketCount;
        this.completedTodayCount = completedTodayCount;
        this.activeTicketCount = activeTicketCount;
        this.available = available;
        this.department = department;
    }

    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double value) { this.averageRating = value; }
    public int getCompletedTicketCount() { return completedTicketCount; }
    public void setCompletedTicketCount(int value) { this.completedTicketCount = value; }
    public int getCompletedTodayCount() { return completedTodayCount; }
    public void setCompletedTodayCount(int value) { this.completedTodayCount = value; }
    public int getActiveTicketCount() { return activeTicketCount; }
    public void setActiveTicketCount(int value) { this.activeTicketCount = value; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean value) { this.available = value; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department value) { this.department = value; }

    public void incrementActiveTicketCount() {
        if (canTakeMoreTickets()) this.activeTicketCount++;
    }
    public void decrementActiveTicketCount() { if (activeTicketCount > 0) activeTicketCount--; }
    public boolean canTakeMoreTickets() { return available && isActive(); }
    public void registerCompletedTicket(double rating) {
        if (!Double.isFinite(rating) || rating < 0.0 || rating > 5.0) throw new IllegalArgumentException("Оценка должна быть от 0.0 до 5.0");
        double total = averageRating * completedTicketCount;
        completedTicketCount++;
        completedTodayCount++;
        averageRating = (total + rating) / completedTicketCount;
    }
    public void resetCompletedTodayCount() { completedTodayCount = 0; }

    @Override
    public String getDisplayName() {
        String fullName = getFullName();
        return fullName != null && !fullName.isBlank()
                ? "Специалист " + fullName + " (" + getUsername() + ")"
                : "Специалист (" + getUsername() + ")";
    }
}