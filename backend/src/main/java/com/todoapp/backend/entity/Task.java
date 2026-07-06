package com.todoapp.backend.entity;

import com.todoapp.backend.enums.TaskStatus;
import lombok.*;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Task entity - represents a task/todo item
 */
@Entity
@Table(name = "tasks", indexes = {
        @Index(name = "idx_user_start_time", columnList = "user_id,start_time")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column
    private LocalDateTime endTime;

    @Column
    private LocalDateTime deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TaskStatus status = TaskStatus.PENDING;

    @Column
    private Integer reminderBeforeStart; // in minutes

    @Column
    private Integer reminderBeforeEnd; // in minutes

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationship with User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude // Avoid infinite recursion when converting to string
    @EqualsAndHashCode.Exclude // Avoid infinite recursion in equals/hashCode
    private User user;

    /**
     * Called before entity is persisted
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /**
     * Called before entity is updated
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}


