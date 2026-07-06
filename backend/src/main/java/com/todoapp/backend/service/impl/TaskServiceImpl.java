package com.todoapp.backend.service.impl;

import com.todoapp.backend.dto.request.TaskCreateRequest;
import com.todoapp.backend.dto.request.TaskUpdateRequest;
import com.todoapp.backend.dto.response.TaskResponse;
import com.todoapp.backend.entity.Task;
import com.todoapp.backend.entity.User;
import com.todoapp.backend.enums.TaskStatus;
import com.todoapp.backend.exception.AccessDeniedException;
import com.todoapp.backend.exception.BadRequestException;
import com.todoapp.backend.exception.ResourceNotFoundException;
import com.todoapp.backend.repository.TaskRepository;
import com.todoapp.backend.repository.UserRepository;
import com.todoapp.backend.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation for TaskService
 */
@Service
@Transactional
public class TaskServiceImpl implements TaskService {

    private static final Logger logger = LoggerFactory.getLogger(TaskServiceImpl.class);

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Get current authenticated username
     */
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getName();
    }

    /**
     * Resolve current user entity
     */
    private User getCurrentUser() {
        String username = getCurrentUsername();
        if (username == null) {
            throw new BadRequestException("UNAUTHENTICATED", "User is not authenticated");
        }
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }

    /**
     * Map Task entity to TaskResponse DTO
     */
    private TaskResponse toResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .startTime(task.getStartTime())
                .endTime(task.getEndTime())
                .deadline(task.getDeadline())
                .status(task.getStatus())
                .reminderBeforeStart(task.getReminderBeforeStart())
                .reminderBeforeEnd(task.getReminderBeforeEnd())
                .userId(task.getUser() != null ? task.getUser().getId() : null)
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    @Override
    public List<TaskResponse> getAllTasksForCurrentUser() {
        User user = getCurrentUser();
        List<Task> tasks = taskRepository.findByUserId(user.getId());
        return tasks.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public TaskResponse getTaskById(Long id) {
        User user = getCurrentUser();
        Task task = taskRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", id));
        return toResponse(task);
    }

    @Override
    public TaskResponse createTask(TaskCreateRequest request) {
        User user = getCurrentUser();

        if (request == null || !request.isValid()) {
            throw new BadRequestException("INVALID_TASK", "Task request is invalid");
        }

        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .deadline(request.getDeadline())
                .status(request.getStatus() != null ? request.getStatus() : TaskStatus.PENDING)
                .reminderBeforeStart(request.getReminderBeforeStart())
                .reminderBeforeEnd(request.getReminderBeforeEnd())
                .user(user)
                .build();

        Task saved = taskRepository.save(task);
        logger.info("Task created (id={}) by user {}", saved.getId(), user.getUsername());

        return toResponse(saved);
    }

    @Override
    public TaskResponse updateTask(Long id, TaskUpdateRequest request) {
        if (request == null || !request.hasAnyFieldSet() || !request.isValid()) {
            throw new BadRequestException("INVALID_TASK", "Task update request is invalid");
        }

        User user = getCurrentUser();
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", id));

        // Ownership check
        if (!task.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not have permission to update this task");
        }

        // Update fields if provided
        if (request.getTitle() != null) task.setTitle(request.getTitle());
        if (request.getDescription() != null) task.setDescription(request.getDescription());
        if (request.getStartTime() != null) task.setStartTime(request.getStartTime());
        if (request.getEndTime() != null) task.setEndTime(request.getEndTime());
        if (request.getDeadline() != null) task.setDeadline(request.getDeadline());
        if (request.getStatus() != null) task.setStatus(request.getStatus());
        if (request.getReminderBeforeStart() != null) task.setReminderBeforeStart(request.getReminderBeforeStart());
        if (request.getReminderBeforeEnd() != null) task.setReminderBeforeEnd(request.getReminderBeforeEnd());

        Task updated = taskRepository.save(task);
        logger.info("Task updated (id={}) by user {}", updated.getId(), user.getUsername());

        return toResponse(updated);
    }

    @Override
    public void deleteTask(Long id) {
        User user = getCurrentUser();
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", id));

        if (!task.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not have permission to delete this task");
        }

        taskRepository.delete(task);
        logger.info("Task deleted (id={}) by user {}", id, user.getUsername());
    }

    @Override
    public TaskResponse markComplete(Long id) {
        User user = getCurrentUser();
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", id));

        if (!task.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not have permission to modify this task");
        }

        task.setStatus(TaskStatus.COMPLETED);
        Task saved = taskRepository.save(task);
        logger.info("Task marked completed (id={}) by user {}", id, user.getUsername());
        return toResponse(saved);
    }

    @Override
    public TaskResponse markUncomplete(Long id) {
        User user = getCurrentUser();
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", id));

        if (!task.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not have permission to modify this task");
        }

        task.setStatus(TaskStatus.PENDING);
        Task saved = taskRepository.save(task);
        logger.info("Task marked uncomplete (id={}) by user {}", id, user.getUsername());
        return toResponse(saved);
    }

    @Override
    public List<TaskResponse> getTasksForCurrentUserByDate(LocalDate date) {
        if (date == null) throw new BadRequestException("INVALID_DATE", "Date cannot be null");
        User user = getCurrentUser();

        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.atTime(LocalTime.MAX);

        List<Task> tasks = taskRepository.findTasksByUserAndDate(user.getId(), dayStart, dayEnd);
        return tasks.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<TaskResponse> getTasksForCurrentUserByMonth(YearMonth month) {
        if (month == null) throw new BadRequestException("INVALID_MONTH", "Month cannot be null");
        User user = getCurrentUser();

        LocalDate first = month.atDay(1);
        LocalDate last = month.atEndOfMonth();

        LocalDateTime monthStart = first.atStartOfDay();
        LocalDateTime monthEnd = last.atTime(LocalTime.MAX);

        List<Task> tasks = taskRepository.findTasksByUserAndMonth(user.getId(), monthStart, monthEnd);
        return tasks.stream().map(this::toResponse).collect(Collectors.toList());
    }
}

