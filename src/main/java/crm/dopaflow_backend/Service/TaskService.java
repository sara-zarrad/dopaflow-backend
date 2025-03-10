package crm.dopaflow_backend.Service;

import crm.dopaflow_backend.Model.*;
import crm.dopaflow_backend.Repository.NotificationRepository;
import crm.dopaflow_backend.Repository.OpportunityRepository;
import crm.dopaflow_backend.Repository.TaskRepository;
import crm.dopaflow_backend.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final OpportunityRepository opportunityRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    private Sort parseSort(String sort) {
        try {
            String[] parts = sort.split(",");
            return Sort.by(Sort.Direction.fromString(parts[1]), parts[0]);
        } catch (Exception e) {
            return Sort.by(Sort.Direction.DESC, "deadline");
        }
    }

    private Date parseDate(String dateStr, boolean isStart) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return isStart
                    ? Date.from(LocalDateTime.now().minusYears(1).atZone(ZoneId.systemDefault()).toInstant())
                    : Date.from(LocalDateTime.now().plusDays(1).atZone(ZoneId.systemDefault()).toInstant());
        }
        try {
            return Date.from(LocalDateTime.parse(dateStr).atZone(ZoneId.systemDefault()).toInstant());
        } catch (Exception e) {
            System.out.println("Date parse error: " + dateStr);
            throw new RuntimeException("Invalid date format. Expected: yyyy-MM-dd'T'HH:mm:ss", e);
        }
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("No authenticated user found");
        }
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Current user not found: " + username));
    }

    private boolean hasAdminPrivileges(User user) {
        return user.getRole() == Role.Admin || user.getRole() == Role.SuperAdmin;
    }

    private void createNotification(User assignedUser, Task task) {
        Notification notification = Notification.builder()
                .message("You have been assigned a new task: " + task.getTitle() + task.getTypeTask() + task.getStatutTask() + task.getDescription())
                .timestamp(LocalDateTime.now())
                .isRead(false)
                .user(assignedUser)
                .task(task)
                .type(Notification.NotificationType.TASK_ASSIGNED)
                .typeString("TASK_ASSIGNED")
                .build();
        notificationRepository.save(notification);
    }

    @Transactional
    public Task createTask(Task task, Long opportunityId, Long assignedUserId) {
        User currentUser = getCurrentUser();
        Opportunity opportunity = opportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new RuntimeException("Opportunity not found with id: " + opportunityId));
        User assignedUser = userRepository.findById(assignedUserId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + assignedUserId));
        if (!hasAdminPrivileges(currentUser)) {
            throw new RuntimeException("You don't have the required privileges to create a task");
        }
        task.setOpportunity(opportunity);
        task.setAssignedUser(assignedUser);
        if (task.getStatutTask() == null) {
            task.setStatutTask(StatutTask.ToDo); // Ensure default if not provided
        }

        Task savedTask = taskRepository.save(task);
        createNotification(assignedUser, savedTask);
        return savedTask;
    }

    @Transactional(readOnly = true)
    public Page<Task> getAllTasks(int page, int size, String sort) {
        User currentUser = getCurrentUser();
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        return hasAdminPrivileges(currentUser)
                ? taskRepository.findAll(pageable)
                : taskRepository.findByAssignedUserId(currentUser.getId(), pageable);
    }

    @Transactional(readOnly = true)
    public List<Task> getAllTasks() {
        User currentUser = getCurrentUser();
        return hasAdminPrivileges(currentUser)
                ? taskRepository.findAll()
                : taskRepository.findByAssignedUserId(currentUser.getId());
    }

    @Transactional(readOnly = true)
    public Optional<Task> getTaskById(Long id) {
        User currentUser = getCurrentUser();
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));
        if (!hasAdminPrivileges(currentUser) && !task.getAssignedUser().getId().equals(currentUser.getId())) {
            throw new SecurityException("You can only view your own tasks");
        }
        return Optional.of(task);
    }

    @Transactional(readOnly = true)
    public List<Task> getTaskByOpportunityId(Long opportunityId) {
        return taskRepository.findByOpportunityId(opportunityId);
    }

    @Transactional
    public Task updateTask(Long id, Task taskDetails, Long assignedUserId) {
        User currentUser = getCurrentUser();
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));
        User previousAssignedUser = task.getAssignedUser();
        if (assignedUserId != null) {
            if (!hasAdminPrivileges(currentUser) && !currentUser.getId().equals(assignedUserId)) {
                throw new SecurityException("Regular users can only assign tasks to themselves");
            }
            User assignedUser = userRepository.findById(assignedUserId)
                    .orElseThrow(() -> new RuntimeException("User not found with id: " + assignedUserId));
            task.setAssignedUser(assignedUser);
            if (!previousAssignedUser.getId().equals(assignedUserId)) {
                createNotification(assignedUser, task);
            }
        }

        task.setTitle(taskDetails.getTitle());
        task.setDescription(taskDetails.getDescription());
        task.setDeadline(taskDetails.getDeadline());
        task.setPriority(taskDetails.getPriority());
        task.setStatutTask(taskDetails.getStatutTask());
        task.setTypeTask(taskDetails.getTypeTask());

        if (taskDetails.getOpportunity() != null) {
            Opportunity opportunity = opportunityRepository.findById(taskDetails.getOpportunity().getId())
                    .orElseThrow(() -> new RuntimeException("Opportunity not found with id: " + taskDetails.getOpportunity().getId()));
            task.setOpportunity(opportunity);
        }
        return taskRepository.save(task);
    }

    @Transactional
    public void deleteTask(Long id) {
        User currentUser = getCurrentUser();
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));
        if (!hasAdminPrivileges(currentUser) && !task.getAssignedUser().getId().equals(currentUser.getId())) {
            throw new SecurityException("You can only delete your own tasks");
        }
        taskRepository.delete(task);
    }

    @Transactional
    public Task updateTaskStatus(Long id, StatutTask newStatus) {
        User currentUser = getCurrentUser();
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));
        if (!hasAdminPrivileges(currentUser) && !task.getAssignedUser().getId().equals(currentUser.getId())) {
            throw new SecurityException("You can only update the status of your own tasks");
        }
        task.setStatutTask(newStatus);
        return taskRepository.save(task);
    }

    @Transactional(readOnly = true)
    public Page<Task> searchTasks(String query, int page, int size, String sort) {
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        return taskRepository.findByTitleContainingIgnoreCase(query, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Task> filterTasks(
            String status,
            String startDateStr,
            String endDateStr,
            Long assignedUserId,
            boolean unassignedOnly,
            int page,
            int size,
            String sort) {
        User currentUser = getCurrentUser();
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        Date startDate = parseDate(startDateStr, true);
        Date endDate = parseDate(endDateStr, false);
        String filteredStatus = (status != null && !status.trim().isEmpty()) ? status : "ANY";

        if (!hasAdminPrivileges(currentUser) && assignedUserId != null &&
                !currentUser.getId().equals(assignedUserId)) {
            throw new SecurityException("Regular users can only filter their own tasks");
        }

        if ("ANY".equals(filteredStatus)) {
            if (unassignedOnly) {
                return taskRepository.findByAssignedUserIsNullAndDeadlineBetween(startDate, endDate, pageable);
            } else if (assignedUserId != null) {
                return taskRepository.findByAssignedUserIdAndDeadlineBetween(assignedUserId, startDate, endDate, pageable);
            } else {
                return hasAdminPrivileges(currentUser)
                        ? taskRepository.findByDeadlineBetween(startDate, endDate, pageable)
                        : taskRepository.findByAssignedUserIdAndDeadlineBetween(currentUser.getId(), startDate, endDate, pageable);
            }
        } else {
            StatutTask statutTask = StatutTask.valueOf(filteredStatus);
            if (unassignedOnly) {
                return taskRepository.findByStatutTaskAndAssignedUserIsNullAndDeadlineBetween(statutTask, startDate, endDate, pageable);
            } else if (assignedUserId != null) {
                return taskRepository.findByStatutTaskAndAssignedUserIdAndDeadlineBetween(statutTask, assignedUserId, startDate, endDate, pageable);
            } else {
                return hasAdminPrivileges(currentUser)
                        ? taskRepository.findByStatutTaskAndDeadlineBetween(statutTask, startDate, endDate, pageable)
                        : taskRepository.findByStatutTaskAndAssignedUserIdAndDeadlineBetween(statutTask, currentUser.getId(), startDate, endDate, pageable);
            }
        }
    }
}