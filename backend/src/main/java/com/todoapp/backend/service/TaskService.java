package com.todoapp.backend.service;

import com.todoapp.backend.dto.request.TaskCreateRequest;
import com.todoapp.backend.dto.request.TaskUpdateRequest;
import com.todoapp.backend.dto.response.TaskResponse;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Service for managing tasks
 */
public interface TaskService {

    List<TaskResponse> getAllTasksForCurrentUser();

    TaskResponse getTaskById(Long id);

    TaskResponse createTask(TaskCreateRequest request);

    TaskResponse updateTask(Long id, TaskUpdateRequest request);

    void deleteTask(Long id);

    TaskResponse markComplete(Long id);

    TaskResponse markUncomplete(Long id);

    List<TaskResponse> getTasksForCurrentUserByDate(LocalDate date);

    List<TaskResponse> getTasksForCurrentUserByMonth(YearMonth month);
}

