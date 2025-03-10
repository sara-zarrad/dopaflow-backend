package crm.dopaflow_backend.Controller;

import crm.dopaflow_backend.Model.StatutTask;
import crm.dopaflow_backend.Model.Task;
import crm.dopaflow_backend.Model.User;
import crm.dopaflow_backend.Service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class TaskController {
    private final TaskService taskService;

    @GetMapping("/all")
    public ResponseEntity<Page<Task>> getAllTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "deadline,desc") String sort) {
        return ResponseEntity.ok(taskService.getAllTasks(page, size, sort));
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<Task> getTask(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(taskService.getTaskById(id)
                    .orElseThrow(() -> new RuntimeException("Task not found with id: " + id)));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @PostMapping("/add")
    public ResponseEntity<?> createTask(
            @RequestBody Task task,
            @RequestParam Long opportunityId,
            @RequestParam Long assignedUserId,
            @AuthenticationPrincipal User currentUser,
            BindingResult bindingResult) {
        System.out.println("Received: Task=" + task + ", opportunityId=" + opportunityId + ", assignedUserId=" + assignedUserId + ", currentUser=" + (currentUser != null ? currentUser.getUsername() : "null"));

        if (bindingResult.hasErrors()) {
            String errors = bindingResult.getAllErrors()
                    .stream()
                    .map(error -> error.getDefaultMessage())
                    .collect(Collectors.joining(", "));
            System.out.println("Validation errors: " + errors);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Validation failed: " + errors);
        }

        try {
            Task createdTask = taskService.createTask(task, opportunityId, assignedUserId);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdTask);
        } catch (RuntimeException e) {
            System.out.println("Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<Task> updateTask(
            @PathVariable Long id,
            @RequestBody Task taskDetails,
            @RequestParam(required = false) Long assignedUserId) {
        try {
            return ResponseEntity.ok(taskService.updateTask(id, taskDetails, assignedUserId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        try {
            taskService.deleteTask(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PutMapping("/change-status/{id}")
    public ResponseEntity<Task> changeStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        try {
            StatutTask newStatus = StatutTask.valueOf(status);
            return ResponseEntity.ok(taskService.updateTaskStatus(id, newStatus));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @GetMapping("/filter")
    public ResponseEntity<Page<Task>> filterTasks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long assignedUserId,
            @RequestParam(defaultValue = "false") boolean unassignedOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "deadline,desc") String sort) {
        return ResponseEntity.ok(taskService.filterTasks(status, startDate, endDate, assignedUserId, unassignedOnly, page, size, sort));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<Task>> searchTasks(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "title,asc") String sort) {
        return ResponseEntity.ok(taskService.searchTasks(query, page, size, sort));
    }

    @GetMapping("/opportunity/{opportunityId}")
    public ResponseEntity<List<Task>> getTasksByOpportunityId(@PathVariable Long opportunityId) {
        return ResponseEntity.ok(taskService.getTaskByOpportunityId(opportunityId));
    }
}