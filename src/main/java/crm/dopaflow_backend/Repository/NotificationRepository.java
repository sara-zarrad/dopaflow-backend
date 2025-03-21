package crm.dopaflow_backend.Repository;

import crm.dopaflow_backend.Model.Notification;
import crm.dopaflow_backend.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserAndIsReadFalseOrderByTimestampDesc(User user);

    List<Notification> findByUserOrderByTimestampDesc(User user);

    long countByUserAndIsReadFalse(User user);

    List<Notification> findByUserId(Long userId);
}
