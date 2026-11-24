package com.k05es.ticketservice.repository;

import com.k05es.ticketservice.model.User;
import com.k05es.ticketservice.model.UserRole;
import com.k05es.ticketservice.model.UserStatus;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Репозиторий для доступа к пользователям в базе данных. */
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    List<User> findByRole(UserRole role);

    @Override
    java.util.Optional<User> findById(Long id);

    @Override
    List<User> findAll();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    java.util.Optional<User> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.role = :role")
    List<User> findByRoleForUpdate(@Param("role") UserRole role);

    boolean existsByUsernameIgnoreCase(String username);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByUsernameIgnoreCaseAndIdNot(String username, Long id);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    User findByUsernameIgnoreCase(String username);
    User findByEmailIgnoreCase(String email);
    default List<User> searchUsers(UserRole role, UserStatus status, String username, String email) {
        Specification<User> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (role != null) {
                predicates.add(criteriaBuilder.equal(root.get("role"), role));
            }
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (username != null) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("username")),
                        "%" + username.toLowerCase(Locale.ROOT) + "%"));
            }
            if (email != null) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("email")),
                        "%" + email.toLowerCase(Locale.ROOT) + "%"));
            }
            query.orderBy(criteriaBuilder.asc(root.get("id")));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        return findAll(specification);
    }

    @Query("select case when count(t) > 0 then true else false end from Ticket t where t.customer.id = :userId or t.specialist.id = :userId")
    boolean isUsedInTickets(@Param("userId") Long userId);

    @Query("select case when count(m) > 0 then true else false end from Message m where m.sender.id = :userId")
    boolean hasSentMessages(@Param("userId") Long userId);

    @Query("select case when count(a) > 0 then true else false end from AdminUser a join a.responsibleForSpecialists specialistId where specialistId = :userId")
    boolean isReferencedByAdministrator(@Param("userId") Long userId);
}