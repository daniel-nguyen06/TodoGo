package com.todoapp.backend.controller;

import com.todoapp.backend.dto.request.TaskCreateRequest;
import com.todoapp.backend.dto.request.TaskUpdateRequest;
import com.todoapp.backend.dto.response.ApiResponse;
import com.todoapp.backend.dto.response.TaskResponse;
import com.todoapp.backend.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    @Autowired
    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getAllTasks() {
        List<TaskResponse> tasks = taskService.getAllTasksForCurrentUser();
        return ResponseEntity.ok(ApiResponse.success("Tasks retrieved", tasks));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponse>> getTaskById(@PathVariable("id") Long id) {
        TaskResponse response = taskService.getTaskById(id);
        return ResponseEntity.ok(ApiResponse.success("Task retrieved", response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(@Valid @RequestBody TaskCreateRequest request) {
        TaskResponse created = taskService.createTask(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(ApiResponse.success("Task created", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTask(@PathVariable("id") Long id,
                                                                 @Valid @RequestBody TaskUpdateRequest request) {
        TaskResponse updated = taskService.updateTask(id, request);
        return ResponseEntity.ok(ApiResponse.success("Task updated", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTask(@PathVariable("id") Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<TaskResponse>> markComplete(@PathVariable("id") Long id) {
        TaskResponse updated = taskService.markComplete(id);
        return ResponseEntity.ok(ApiResponse.success("Task marked complete", updated));
    }

    @PatchMapping("/{id}/uncomplete")
    public ResponseEntity<ApiResponse<TaskResponse>> markUncomplete(@PathVariable("id") Long id) {
        TaskResponse updated = taskService.markUncomplete(id);
        return ResponseEntity.ok(ApiResponse.success("Task marked uncomplete", updated));
    }

    @GetMapping("/date")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getTasksByDate(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<TaskResponse> tasks = taskService.getTasksForCurrentUserByDate(date);
        return ResponseEntity.ok(ApiResponse.success("Tasks retrieved for date", tasks));
    }

    @GetMapping("/month")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getTasksByMonth(@RequestParam("year") int year,
                                                                           @RequestParam("month") int month) {
        YearMonth ym = YearMonth.of(year, month);
        List<TaskResponse> tasks = taskService.getTasksForCurrentUserByMonth(ym);
        return ResponseEntity.ok(ApiResponse.success("Tasks retrieved for month", tasks));
    }
}

