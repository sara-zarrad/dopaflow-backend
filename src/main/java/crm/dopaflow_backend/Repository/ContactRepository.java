package crm.dopaflow_backend.Repository;

import crm.dopaflow_backend.Model.Contact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface ContactRepository extends JpaRepository<Contact, Long> {

        Page<Contact> findAll(Pageable pageable);

        Page<Contact> findByNameContainingIgnoreCase(String name, Pageable pageable);

        Page<Contact> findByStatusAndOwnerIdAndCreatedAtBetween(
                String status,
                Long ownerId,
                LocalDateTime startDate,
                LocalDateTime endDate,
                Pageable pageable
        );

        Page<Contact> findByStatusAndCreatedAtBetween(
                String status,
                LocalDateTime startDate,
                LocalDateTime endDate,
                Pageable pageable
        );

        Page<Contact> findByStatusAndOwnerIsNullAndCreatedAtBetween(
                String status,
                LocalDateTime startDate,
                LocalDateTime endDate,
                Pageable pageable
        );

        Page<Contact> findByOwnerIsNull(Pageable pageable);

        // New methods without status filter
        Page<Contact> findByOwnerIdAndCreatedAtBetween(
                Long ownerId,
                LocalDateTime startDate,
                LocalDateTime endDate,
                Pageable pageable
        );

        Page<Contact> findByOwnerIsNullAndCreatedAtBetween(
                LocalDateTime startDate,
                LocalDateTime endDate,
                Pageable pageable
        );

        Page<Contact> findByCreatedAtBetween(
                LocalDateTime startDate,
                LocalDateTime endDate,
                Pageable pageable
        );
}
