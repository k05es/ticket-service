package com.k05es.ticketservice.model;

import java.util.regex.Pattern;

/**
 * Клиент системы.
 * Создаёт заявки и общается со специалистом.
 * Роль — CUSTOMER.
 */
public class CustomerUser extends User {

    // Маска телефона: +7 и ровно 10 цифр
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+7\\d{10}$");

    // Контактный телефон в формате +7XXXXXXXXXX
    private String phone;

    // Название компании (необязательно)
    private String companyName;

    // Адрес
    private String address;

    // Предпочитаемый способ связи
    private ContactPreference preferredContactMethod;

    // Количество созданных заявок
    private int createdTicketCount;

    /**
     * Конструктор по умолчанию.
     * Нужен фреймворкам, которым требуется пустой объект.
     */
    protected CustomerUser() {
        super();
    }

    /**
     * Конструктор со всеми полями.
     */
    public CustomerUser(Long id, String username, String email, String passHash,
                        UserStatus status,
                        String lastName, String firstName, String middleName,
                        String phone,
                        String companyName,
                        String address,
                        ContactPreference preferredContactMethod,
                        int createdTicketCount) {
        super(id, username, email, passHash, UserRole.CUSTOMER, status,
                lastName, firstName, middleName);
        setPhone(phone);
        this.companyName = companyName;
        this.address = address;
        this.preferredContactMethod = preferredContactMethod;
        this.createdTicketCount = createdTicketCount;
    }

    // Геттеры и сеттеры

    public String getPhone() {
        return phone;
    }

    /**
     * Устанавливает телефон. Принимает только формат +7XXXXXXXXXX.
     */
    public void setPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Телефон обязателен");
        }
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new IllegalArgumentException(
                    "Телефон должен быть в формате +7XXXXXXXXXX: " + phone);
        }
        this.phone = phone;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public ContactPreference getPreferredContactMethod() {
        return preferredContactMethod;
    }

    public void setPreferredContactMethod(ContactPreference preferredContactMethod) {
        this.preferredContactMethod = preferredContactMethod;
    }

    public int getCreatedTicketCount() {
        return createdTicketCount;
    }

    public void setCreatedTicketCount(int createdTicketCount) {
        this.createdTicketCount = createdTicketCount;
    }

    /**
     * Проверяет, является ли клиент представителем компании.
     */
    public boolean isCompanyCustomer() {
        return this.companyName != null && !this.companyName.isBlank();
    }

    /**
     * Увеличивает счётчик созданных заявок.
     */
    public void incrementCreatedTicketCount() {
        this.createdTicketCount++;
    }

    /**
     * Возвращает отображаемое имя клиента.
     */
    @Override
    public String getDisplayName() {
        String fullName = getFullName();
        if (fullName != null && !fullName.isBlank()) {
            return "Клиент " + fullName + " (" + getUsername() + ")";
        }
        return "Клиент (" + getUsername() + ")";
    }

    /**
     * Краткое строковое представление клиента.
     */
    @Override
    public String toString() {
        return "CustomerUser{" +
                "id=" + getId() +
                ", username='" + getUsername() + '\'' +
                ", fullName='" + getFullName() + '\'' +
                ", email='" + getEmail() + '\'' +
                ", status=" + getStatus() +
                ", phone='" + phone + '\'' +
                ", companyName='" + companyName + '\'' +
                ", address='" + address + '\'' +
                ", preferredContactMethod=" + preferredContactMethod +
                ", createdTicketCount=" + createdTicketCount +
                '}';
    }
}