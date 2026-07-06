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
 * DTO for updating an existing task
 * All fields are optional - only provided fields will be updated
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskUpdateRequest {

    @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
    private String title;

    private String description;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime deadline;

    private TaskStatus status;

    @Min(value = 0, message = "Reminder before start must be greater than or equal to 0")
    @Max(value = 10080, message = "Reminder before start cannot exceed 7 days (10080 minutes)")
    private Integer reminderBeforeStart;

    @Min(value = 0, message = "Reminder before end must be greater than or equal to 0")
    @Max(value = 10080, message = "Reminder before end cannot exceed 7 days (10080 minutes)")
    private Integer reminderBeforeEnd;

    /**
     * Validate that if both startTime and endTime are provided, endTime is not before startTime
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        // If both times are provided, endTime must be after or equal to startTime
        if (startTime != null && endTime != null && endTime.isBefore(startTime)) {
            return false;
        }
        return true;
    }

    /**
     * Check if update request has any fields set
     * @return true if at least one field is not null
     */
    public boolean hasAnyFieldSet() {
        return title != null ||
                description != null ||
                startTime != null ||
                endTime != null ||
                deadline != null ||
                status != null ||
                reminderBeforeStart != null ||
                reminderBeforeEnd != null;
    }
}

