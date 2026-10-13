package com.k05es.ticketservice.model;

/**
 * Специалист системы.
 * Обрабатывает заявки клиентов.
 * Роль — SPECIALIST.
 */
public class SpecialistUser extends User {

    // Средняя оценка работы
    private double averageRating;

    // Количество завершённых заявок за всё время
    private int completedTicketCount;

    // Количество завершённых заявок за сегодня
    private int completedTodayCount;

    // Количество активных заявок
    private int activeTicketCount;

    // Доступность для новых заявок
    private boolean available;

    // Отдел
    private Department department;

    /**
     * Конструктор по умолчанию.
     * Нужен фреймворкам, которым требуется пустой объект.
     */
    protected SpecialistUser() {
        super();
        this.available = true;
    }

    /**
     * Конструктор со всеми полями.
     */
    public SpecialistUser(Long id, String username, String email, String passHash,
                          UserStatus status,
                          String lastName, String firstName, String middleName,
                          double averageRating,
                          int completedTicketCount,
                          int completedTodayCount,
                          int activeTicketCount,
                          boolean available,
                          Department department) {
        super(id, username, email, passHash, UserRole.SPECIALIST, status,
                lastName, firstName, middleName);
        this.averageRating = averageRating;
        this.completedTicketCount = completedTicketCount;
        this.completedTodayCount = completedTodayCount;
        this.activeTicketCount = activeTicketCount;
        this.available = available;
        this.department = department;
    }

    // Геттеры и сеттеры

    public double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }

    public int getCompletedTicketCount() {
        return completedTicketCount;
    }

    public void setCompletedTicketCount(int completedTicketCount) {
        this.completedTicketCount = completedTicketCount;
    }

    public int getCompletedTodayCount() {
        return completedTodayCount;
    }

    public void setCompletedTodayCount(int completedTodayCount) {
        this.completedTodayCount = completedTodayCount;
    }

    public int getActiveTicketCount() {
        return activeTicketCount;
    }

    public void setActiveTicketCount(int activeTicketCount) {
        this.activeTicketCount = activeTicketCount;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    /**
     * Увеличивает счётчик активных заявок.
     */
    public void incrementActiveTicketCount() {
        if (canTakeMoreTickets()) {
            this.activeTicketCount++;
        }
    }

    /**
     * Уменьшает счётчик активных заявок.
     */
    public void decrementActiveTicketCount() {
        if (this.activeTicketCount > 0) {
            this.activeTicketCount--;
        }
    }

    /**
     * Проверяет, может ли специалист взять ещё заявку.
     */
    public boolean canTakeMoreTickets() {
        return this.available;
    }

    /**
     * Регистрирует завершённую заявку и пересчитывает средний рейтинг.
     */
    public void registerCompletedTicket(double rating) {
        if (rating < 0.0 || rating > 5.0) {
            throw new IllegalArgumentException("Оценка должна быть от 0.0 до 5.0");
        }
        double total = this.averageRating * this.completedTicketCount;
        this.completedTicketCount++;
        this.completedTodayCount++;
        this.averageRating = (total + rating) / this.completedTicketCount;
    }

    /**
     * Сбрасывает счётчик завершённых заявок за сегодня.
     */
    public void resetCompletedTodayCount() {
        this.completedTodayCount = 0;
    }

    /**
     * Возвращает отображаемое имя специалиста.
     */
    @Override
    public String getDisplayName() {
        String fullName = getFullName();
        if (fullName != null && !fullName.isBlank()) {
            return "Специалист " + fullName + " (" + getUsername() + ")";
        }
        return "Специалист (" + getUsername() + ")";
    }

    /**
     * Краткое строковое представление специалиста.
     */
    @Override
    public String toString() {
        return "SpecialistUser{" +
                "id=" + getId() +
                ", username='" + getUsername() + '\'' +
                ", fullName='" + getFullName() + '\'' +
                ", email='" + getEmail() + '\'' +
                ", status=" + getStatus() +
                ", averageRating=" + averageRating +
                ", completedTicketCount=" + completedTicketCount +
                ", completedTodayCount=" + completedTodayCount +
                ", activeTicketCount=" + activeTicketCount +
                ", available=" + available +
                ", department=" + department +
                '}';
    }
}