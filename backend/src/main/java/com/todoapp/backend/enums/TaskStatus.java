package com.todoapp.backend.enums;

/**
 * Enum for Task status
 */
public enum TaskStatus {
    PENDING("Chưa làm"),
    COMPLETED("Đã hoàn thành"),
    CANCELLED("Đã hủy");

    private final String description;

    TaskStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

