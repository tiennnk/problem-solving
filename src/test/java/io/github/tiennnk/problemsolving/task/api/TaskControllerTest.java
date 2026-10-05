package io.github.tiennnk.problemsolving.task.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import io.github.tiennnk.problemsolving.common.domain.Language;
import io.github.tiennnk.problemsolving.common.domain.ProtocolType;
import io.github.tiennnk.problemsolving.task.application.CreateTaskCommand;
import io.github.tiennnk.problemsolving.task.application.TaskNotFoundException;
import io.github.tiennnk.problemsolving.task.application.TaskService;
import io.github.tiennnk.problemsolving.task.domain.Task;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

  @Autowired
  MockMvc mockMvc;

  @MockitoBean
  TaskService taskService;

  private final Task task = Task.create("Two Sum", Language.JAVA, ProtocolType.PROBLEM_SOLVER);

  @Test
  void createReturns201WithLocation() throws Exception {
    when(taskService.createTask(any(CreateTaskCommand.class))).thenReturn(task);

    mockMvc.perform(post("/api/tasks")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"problem": "Two Sum", "language": "JAVA"}
                """))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "http://localhost/api/tasks/" + task.getId()))
        .andExpect(jsonPath("$.status").value("PENDING"));
  }

  @Test
  void createRejectsInvalidBody() throws Exception {
    mockMvc.perform(post("/api/tasks")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"problem": " "}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.problem").exists())
        .andExpect(jsonPath("$.errors.language").exists());
  }

  @Test
  void createExplainsUnknownLanguage() throws Exception {
    mockMvc.perform(post("/api/tasks")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"problem": "Two Sum", "language": "COBOL"}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail")
            .value("Invalid value 'COBOL' for field 'language'. Allowed values: [JAVA, PHP]"));
  }

  @Test
  void getReturns404WhenMissing() throws Exception {
    UUID id = UUID.randomUUID();
    when(taskService.getTask(id)).thenThrow(new TaskNotFoundException(id));

    mockMvc.perform(get("/api/tasks/{id}", id))
        .andExpect(status().isNotFound());
  }
}
