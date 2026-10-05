package io.github.tiennnk.problemsolving.task.api;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.github.tiennnk.problemsolving.task.application.TaskService;
import io.github.tiennnk.problemsolving.task.domain.Task;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

  private final TaskService taskService;

  public TaskController(TaskService taskService) {
    this.taskService = taskService;
  }

  @PostMapping
  public ResponseEntity<TaskResponse> create(@Valid @RequestBody CreateTaskRequest request) {
    Task task = taskService.createTask(request.toCommand());
    URI location = ServletUriComponentsBuilder.fromCurrentRequest()
        .path("/{id}")
        .buildAndExpand(task.getId())
        .toUri();
    return ResponseEntity.created(location).body(TaskResponse.from(task));
  }

  @GetMapping("/{id}")
  public TaskResponse get(@PathVariable UUID id) {
    return TaskResponse.from(taskService.getTask(id));
  }

  @GetMapping("/{id}/status")
  public TaskStatusResponse status(@PathVariable UUID id) {
    return TaskStatusResponse.from(taskService.getTask(id));
  }
}
