package com.todoapp.backend.repository;

import com.todoapp.backend.entity.Task;
import com.todoapp.backend.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Task entity
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    /**
     * Find all tasks for a specific user with pagination
     * @param userId the user id
     * @param pageable pagination info
     * @return Page of tasks
     */
    Page<Task> findByUserId(Long userId, Pageable pageable);

    /**
     * Find all tasks for a specific user
     * @param userId the user id
     * @return List of tasks
     */
    List<Task> findByUserId(Long userId);

    /**
     * Find task by id and user id (for ownership check)
     * @param id the task id
     * @param userId the user id
     * @return Optional of Task if found and belongs to user
     */
    Optional<Task> findByIdAndUserId(Long id, Long userId);

    /**
     * Find tasks for user on a specific date (including tasks that span multiple days)
     * Matches tasks where:
     * - startTime <= dayEnd AND (endTime IS NULL OR endTime >= dayStart)
     * This includes tasks that start on the day, end on the day, or span across the day
     *
     * @param userId the user id
     * @param dayStart the start of the day (typically 00:00:00 in UTC)
     * @param dayEnd the end of the day (typically 23:59:59 in UTC)
     * @param pageable pagination info
     * @return Page of tasks
     */
    @Query("SELECT t FROM Task t WHERE t.user.id = :userId " +
            "AND t.startTime <= :dayEnd " +
            "AND (t.endTime IS NULL OR t.endTime >= :dayStart) " +
            "ORDER BY t.startTime ASC")
    Page<Task> findTasksByUserAndDate(
            @Param("userId") Long userId,
            @Param("dayStart") LocalDateTime dayStart,
            @Param("dayEnd") LocalDateTime dayEnd,
            Pageable pageable);

    /**
     * Find tasks for user on a specific date (no pagination)
     * @param userId the user id
     * @param dayStart the start of the day
     * @param dayEnd the end of the day
     * @return List of tasks
     */
    @Query("SELECT t FROM Task t WHERE t.user.id = :userId " +
            "AND t.startTime <= :dayEnd " +
            "AND (t.endTime IS NULL OR t.endTime >= :dayStart) " +
            "ORDER BY t.startTime ASC")
    List<Task> findTasksByUserAndDate(
            @Param("userId") Long userId,
            @Param("dayStart") LocalDateTime dayStart,
            @Param("dayEnd") LocalDateTime dayEnd);

    /**
     * Find tasks for user in a specific month
     * @param userId the user id
     * @param monthStart the start of the month (typically 1st day 00:00:00 in UTC)
     * @param monthEnd the end of the month (typically last day 23:59:59 in UTC)
     * @param pageable pagination info
     * @return Page of tasks
     */
    @Query("SELECT t FROM Task t WHERE t.user.id = :userId " +
            "AND t.startTime <= :monthEnd " +
            "AND (t.endTime IS NULL OR t.endTime >= :monthStart) " +
            "ORDER BY t.startTime ASC")
    Page<Task> findTasksByUserAndMonth(
            @Param("userId") Long userId,
            @Param("monthStart") LocalDateTime monthStart,
            @Param("monthEnd") LocalDateTime monthEnd,
            Pageable pageable);

    /**
     * Find tasks for user in a specific month (no pagination)
     * @param userId the user id
     * @param monthStart the start of the month
     * @param monthEnd the end of the month
     * @return List of tasks
     */
    @Query("SELECT t FROM Task t WHERE t.user.id = :userId " +
            "AND t.startTime <= :monthEnd " +
            "AND (t.endTime IS NULL OR t.endTime >= :monthStart) " +
            "ORDER BY t.startTime ASC")
    List<Task> findTasksByUserAndMonth(
            @Param("userId") Long userId,
            @Param("monthStart") LocalDateTime monthStart,
            @Param("monthEnd") LocalDateTime monthEnd);

    /**
     * Find tasks for user by status
     * @param userId the user id
     * @param status the task status
     * @param pageable pagination info
     * @return Page of tasks
     */
    Page<Task> findByUserIdAndStatus(Long userId, TaskStatus status, Pageable pageable);

    /**
     * Count tasks for user by status
     * @param userId the user id
     * @param status the task status
     * @return count of tasks
     */
    long countByUserIdAndStatus(Long userId, TaskStatus status);

    /**
     * Delete all tasks for a specific user
     * @param userId the user id
     */
    void deleteByUserId(Long userId);

    /**
     * Find tasks for user in a date range
     * @param userId the user id
     * @param startDate the start date
     * @param endDate the end date
     * @param pageable pagination info
     * @return Page of tasks
     */
    @Query("SELECT t FROM Task t WHERE t.user.id = :userId " +
            "AND t.startTime >= :startDate " +
            "AND t.startTime <= :endDate " +
            "ORDER BY t.startTime ASC")
    Page<Task> findTasksByUserAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);
}

