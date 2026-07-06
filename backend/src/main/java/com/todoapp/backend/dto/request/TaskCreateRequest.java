package com.todoapp.backend.dto.request;

import com.todoapp.backend.enums.TaskStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for creating a new task
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskCreateRequest {

    @NotBlank(message = "Title cannot be blank")
    @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
    private String title;

    private String description;

    @NotNull(message = "Start time cannot be null")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime deadline;

    @Builder.Default
    private TaskStatus status = TaskStatus.PENDING;

    @Min(value = 0, message = "Reminder before start must be greater than or equal to 0")
    @Max(value = 10080, message = "Reminder before start cannot exceed 7 days (10080 minutes)")
    private Integer reminderBeforeStart;

    @Min(value = 0, message = "Reminder before end must be greater than or equal to 0")
    @Max(value = 10080, message = "Reminder before end cannot exceed 7 days (10080 minutes)")
    private Integer reminderBeforeEnd;

    /**
     * Validate that startTime is not null and endTime is not before startTime
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        if (startTime == null) {
            return false;
        }
        // If endTime exists, it must be after or equal to startTime
        if (endTime != null && endTime.isBefore(startTime)) {
            return false;
        }
        return true;
    }
}

