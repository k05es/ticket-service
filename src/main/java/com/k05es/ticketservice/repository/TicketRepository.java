package com.k05es.ticketservice.repository;

import com.k05es.ticketservice.model.Ticket;
import com.k05es.ticketservice.model.TicketStatus;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Репозиторий для доступа к заявкам в базе данных. */
public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    @Override
    @EntityGraph(attributePaths = {"customer", "specialist", "messages", "messages.sender"})
    java.util.Optional<Ticket> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"customer", "specialist", "messages", "messages.sender"})
    List<Ticket> findAll();

    @EntityGraph(attributePaths = {"customer", "specialist", "messages", "messages.sender"})
    List<Ticket> findByStatus(TicketStatus status);

    @EntityGraph(attributePaths = {"customer", "specialist", "messages", "messages.sender"})
    List<Ticket> findByCustomerId(Long customerId);

    @EntityGraph(attributePaths = {"customer", "specialist", "messages", "messages.sender"})
    List<Ticket> findBySpecialistId(Long specialistId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"customer", "specialist", "messages", "messages.sender"})
    @Query("select t from Ticket t where t.id = :id")
    java.util.Optional<Ticket> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"customer", "specialist", "messages", "messages.sender"})
    default List<Ticket> searchTickets(TicketStatus status, Long customerId,
                                       Long specialistId, String customerEmail) {
        Specification<Ticket> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (customerId != null) {
                predicates.add(criteriaBuilder.equal(root.get("customer").get("id"), customerId));
            }
            if (specialistId != null) {
                predicates.add(criteriaBuilder.equal(root.get("specialist").get("id"), specialistId));
            }
            if (customerEmail != null) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("customer").get("email")),
                        customerEmail.toLowerCase(Locale.ROOT)));
            }
            query.orderBy(criteriaBuilder.desc(root.get("createdAt")));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        return findAll(specification);
    }

    @Override
    @EntityGraph(attributePaths = {"customer", "specialist", "messages", "messages.sender"})
    List<Ticket> findAll(Specification<Ticket> specification);

    @EntityGraph(attributePaths = {"customer", "specialist"})
    List<Ticket> findByStatusOrderByCreatedAtAsc(TicketStatus status);
}