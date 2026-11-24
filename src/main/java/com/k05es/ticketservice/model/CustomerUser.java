package com.k05es.ticketservice.model;

import com.k05es.ticketservice.exception.InvalidDomainValueException;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.util.regex.Pattern;

/**
 * Клиент системы.
 * Создаёт заявки и общается со специалистом.
 * Роль — CUSTOMER.
 */
@Entity
@DiscriminatorValue("CUSTOMER")
public class CustomerUser extends User {

    // Маска телефона: +7 и ровно 10 цифр
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+7\\d{10}$");

    @Column(length = 12)
    private String phone;
    @Column(name = "company_name", length = 120)
    private String companyName;
    @Column(length = 255)
    private String address;
    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_contact_method", length = 20)
    private ContactPreference preferredContactMethod;
    @Column(name = "created_ticket_count", nullable = false)
    private int createdTicketCount;

    /** Конструктор по умолчанию для JPA. */
    protected CustomerUser() { super(); }

    public CustomerUser(Long id, String username, String email, String passHash,
                        UserStatus status, String lastName, String firstName, String middleName,
                        String phone, String companyName, String address,
                        ContactPreference preferredContactMethod, int createdTicketCount) {
        super(id, username, email, passHash, UserRole.CUSTOMER, status,
                lastName, firstName, middleName);
        setPhone(phone);
        this.companyName = companyName;
        this.address = address;
        this.preferredContactMethod = preferredContactMethod;
        this.createdTicketCount = createdTicketCount;
    }

    public String getPhone() { return phone; }
    public void setPhone(String phone) {
        if (phone == null || phone.isBlank()) throw new InvalidDomainValueException("Телефон обязателен");
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new InvalidDomainValueException("Телефон должен быть в формате +7XXXXXXXXXX: " + phone);
        }
        this.phone = phone;
    }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public ContactPreference getPreferredContactMethod() { return preferredContactMethod; }
    public void setPreferredContactMethod(ContactPreference value) { this.preferredContactMethod = value; }
    public int getCreatedTicketCount() { return createdTicketCount; }
    public void setCreatedTicketCount(int value) { this.createdTicketCount = value; }
    public boolean isCompanyCustomer() { return companyName != null && !companyName.isBlank(); }
    public void incrementCreatedTicketCount() { this.createdTicketCount++; }

    @Override
    public String getDisplayName() {
        String fullName = getFullName();
        return fullName != null && !fullName.isBlank()
                ? "Клиент " + fullName + " (" + getUsername() + ")"
                : "Клиент (" + getUsername() + ")";
    }
}