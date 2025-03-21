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

    @Column(nullable = false)
    private NotificationType type; // Enum for notification types (e.g., PASSWORD_CHANGE, TWO_FA_ENABLED, TWO_FA_DISABLED)

    @Column(nullable = false)
    private boolean isRead = false;

    @Column(nullable = false)
    private String link; // Added
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime timestamp;

    // Enum for notification types
    public enum NotificationType {
        PASSWORD_CHANGE,
        TWO_FA_ENABLED,
        TWO_FA_DISABLED,
        // Add more types as needed for future use cases
        USER_CREATED,
        USER_DELETED,
        CONTACT_CREATED,
        TASK_ASSIGNED
    }
}
