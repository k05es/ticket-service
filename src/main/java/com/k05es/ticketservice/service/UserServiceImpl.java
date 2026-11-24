package com.k05es.ticketservice.service;

import com.k05es.ticketservice.dto.request.UpdateUserRequest;
import com.k05es.ticketservice.exception.DuplicateResourceException;
import com.k05es.ticketservice.exception.InvalidRequestException;
import com.k05es.ticketservice.exception.UserHasDependenciesException;
import com.k05es.ticketservice.exception.UserNotFoundException;
import com.k05es.ticketservice.model.AdminUser;
import com.k05es.ticketservice.model.CustomerUser;
import com.k05es.ticketservice.model.SpecialistUser;
import com.k05es.ticketservice.model.User;
import com.k05es.ticketservice.model.UserRole;
import com.k05es.ticketservice.model.UserStatus;
import com.k05es.ticketservice.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Реализация UserService с использованием Spring Data JPA. */
@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User registerUser(User user) {
        if (user == null) {
            throw new InvalidRequestException("Пользователь не может быть null");
        }
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            throw new InvalidRequestException("Логин не может быть пустым");
        }
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new InvalidRequestException("Email не может быть пустым");
        }
        String normalizedUsername = user.getUsername().trim().toLowerCase(Locale.ROOT);
        String normalizedEmail = user.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw new DuplicateResourceException("Логин уже занят: " + user.getUsername());
        }
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateResourceException("Email уже занят: " + user.getEmail());
        }
        user.setUsername(normalizedUsername);
        user.setEmail(normalizedEmail);
        if (user instanceof AdminUser admin) {
            validateResponsibleSpecialists(admin.getResponsibleForSpecialists());
        }
        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public User findUserById(Long id) {
        if (id == null) {
            throw new UserNotFoundException("Идентификатор пользователя не указан");
        }
        return userRepository.findById(id)
                .orElseThrow(() -> UserNotFoundException.byId(id));
    }

    @Override
    @Transactional(readOnly = true)
    public User findUserByUsername(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        return userRepository.findByUsernameIgnoreCase(username.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public User findUserByEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return userRepository.findByEmailIgnoreCase(email.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findUsersByRole(UserRole role) {
        if (role == null) {
            return List.of();
        }
        return userRepository.findByRole(role);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> searchUsers(UserRole role, UserStatus status, String username, String email) {
        return userRepository.searchUsers(role, status, normalize(username), normalize(email));
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public User updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> UserNotFoundException.byId(id));
        if (request.getUsername() != null) {
            String username = request.getUsername().trim().toLowerCase(Locale.ROOT);
            if (username.isBlank()) {
                throw new InvalidRequestException("Логин не может быть пустым");
            }
            if (userRepository.existsByUsernameIgnoreCaseAndIdNot(username, id)) {
                throw new DuplicateResourceException("Логин уже занят: " + username);
            }
            user.setUsername(username);
        }
        if (request.getEmail() != null) {
            String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
            if (email.isBlank()) {
                throw new InvalidRequestException("Email не может быть пустым");
            }
            if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
                throw new DuplicateResourceException("Email уже занят: " + email);
            }
            user.setEmail(email);
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getMiddleName() != null) {
            user.setMiddleName(request.getMiddleName());
        }

        if (user instanceof CustomerUser customer) {
            if (request.getPhone() != null) {
                customer.setPhone(request.getPhone());
            }
            if (request.getCompanyName() != null) {
                customer.setCompanyName(request.getCompanyName());
            }
            if (request.getAddress() != null) {
                customer.setAddress(request.getAddress());
            }
            if (request.getPreferredContactMethod() != null) {
                customer.setPreferredContactMethod(request.getPreferredContactMethod());
            }
        } else if (user instanceof SpecialistUser specialist) {
            if (request.getDepartment() != null) {
                specialist.setDepartment(request.getDepartment());
            }
            if (request.getAvailable() != null) {
                specialist.setAvailable(request.getAvailable());
            }
        } else if (user instanceof AdminUser admin) {
            if (request.getDepartment() != null) {
                admin.setDepartment(request.getDepartment());
            }
            if (request.getPermissions() != null) {
                admin.setPermissions(request.getPermissions());
            }
            if (request.getResponsibleForSpecialists() != null) {
                validateResponsibleSpecialists(request.getResponsibleForSpecialists());
                admin.setResponsibleForSpecialists(request.getResponsibleForSpecialists());
            }
        }
        return userRepository.save(user);
    }

    @Override
    public boolean deleteUser(Long id) {
        if (id == null || !userRepository.existsById(id)) {
            return false;
        }
        if (userRepository.isUsedInTickets(id) || userRepository.hasSentMessages(id)
                || userRepository.isReferencedByAdministrator(id)) {
            throw new UserHasDependenciesException(
                    "Нельзя удалить пользователя с ID " + id
                            + ", связанного с заявками, сообщениями или администраторами");
        }
        userRepository.deleteById(id);
        return true;
    }

    private void validateResponsibleSpecialists(Set<Long> specialistIds) {
        if (specialistIds == null || specialistIds.isEmpty()) {
            return;
        }
        for (Long specialistId : specialistIds) {
            if (specialistId == null || specialistId <= 0) {
                throw new InvalidRequestException(
                        "ID ответственного специалиста должен быть положительным числом");
            }
        }
        List<User> foundUsers = userRepository.findAllById(specialistIds);
        if (foundUsers.size() != specialistIds.size()) {
            throw new InvalidRequestException("Все ответственные пользователи должны быть специалистами");
        }
        for (User foundUser : foundUsers) {
            if (!(foundUser instanceof SpecialistUser)) {
                throw new InvalidRequestException("Все ответственные пользователи должны быть специалистами");
            }
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
