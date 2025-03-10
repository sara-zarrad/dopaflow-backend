package crm.dopaflow_backend.Model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Notification {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "user_id", nullable = false)
        private User user;

        @Column(nullable = false)
        private String message;

        @Transient
        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        private NotificationType type; // Enum for notification types (e.g., PASSWORD_CHANGE, TWO_FA_ENABLED, TWO_FA_DISABLED)

        @Column(name = "type", nullable = false) // Persisted column for DB
        private String typeString;

        @Column(nullable = false)
        private boolean isRead = false;

        @CreationTimestamp
        @Column(updatable = false)
        private LocalDateTime timestamp;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "task_id")
        private Task task;

        // Enum for notification types
        public enum NotificationType {
            PASSWORD_CHANGE,
            TWO_FA_ENABLED,
            TWO_FA_DISABLED,
            // Add more types as needed for future use cases
            USER_CREATED,
            USER_DELETED,
            CONTACT_CREATED,
            TASK_ASSIGNED,
        }
}
