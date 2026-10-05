package io.github.tiennnk.problemsolving.task.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import io.github.tiennnk.problemsolving.common.domain.Language;
import io.github.tiennnk.problemsolving.common.domain.ProtocolType;
import io.github.tiennnk.problemsolving.support.PostgresTestcontainersConfiguration;
import io.github.tiennnk.problemsolving.task.domain.FailureReason;
import io.github.tiennnk.problemsolving.task.domain.Task;
import io.github.tiennnk.problemsolving.task.domain.TaskStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PostgresTestcontainersConfiguration.class)
class TaskRepositoryTest {

  @Autowired
  TaskRepository taskRepository;

  @Autowired
  EntityManager entityManager;

  private Task newTask() {
    return Task.create("Two Sum", Language.JAVA, ProtocolType.PROBLEM_SOLVER);
  }

  @Test
  void persistsAndLoadsTask() {
    Task saved = taskRepository.saveAndFlush(newTask());
    entityManager.clear();

    Task loaded = taskRepository.findById(saved.getId()).orElseThrow();

    assertThat(loaded).isNotSameAs(saved);
    assertThat(loaded.getProblem()).isEqualTo("Two Sum");
    assertThat(loaded.getLanguage()).isEqualTo(Language.JAVA);
    assertThat(loaded.getProtocolType()).isEqualTo(ProtocolType.PROBLEM_SOLVER);
    assertThat(loaded.getStatus()).isEqualTo(TaskStatus.PENDING);
    assertThat(loaded.getFailureReason()).isNull();
    assertThat(loaded.getVersion()).isZero();
  }

  @Test
  void persistsFailureReason() {
    Task task = newTask();
    task.start();
    task.fail(FailureReason.REPAIR_EXHAUSTED);
    UUID id = taskRepository.saveAndFlush(task).getId();
    entityManager.clear();

    Task loaded = taskRepository.findById(id).orElseThrow();

    assertThat(loaded.getStatus()).isEqualTo(TaskStatus.FAILED);
    assertThat(loaded.getFailureReason()).isEqualTo(FailureReason.REPAIR_EXHAUSTED);
  }

  @Test
  void versionIncrementsOnUpdate() {
    Task task = taskRepository.saveAndFlush(newTask());
    assertThat(task.getVersion()).isZero();

    task.start();
    taskRepository.saveAndFlush(task);

    assertThat(task.getVersion()).isEqualTo(1L);
  }

  @Test
  void staleUpdateIsRejectedByOptimisticLock() {
    UUID id = taskRepository.saveAndFlush(newTask()).getId();
    entityManager.clear();
    Task staleCopy = taskRepository.findById(id).orElseThrow();

    entityManager.createNativeQuery("UPDATE task SET version = version + 1 WHERE id = :id")
        .setParameter("id", id)
        .executeUpdate();

    staleCopy.start();
    assertThatThrownBy(() -> taskRepository.saveAndFlush(staleCopy))
        .isInstanceOf(ObjectOptimisticLockingFailureException.class);
  }

  @Test
  void databaseRejectsFailedTaskWithoutReason() {
    Task task = taskRepository.saveAndFlush(newTask());

    assertThatThrownBy(() -> entityManager
        .createNativeQuery("UPDATE task SET status = 'FAILED' WHERE id = :id")
        .setParameter("id", task.getId())
        .executeUpdate())
        .isInstanceOf(PersistenceException.class)
        .hasStackTraceContaining("ck_task_failure_consistency");
  }

  @Test
  void databaseRejectsUnknownStatus() {
    Task task = taskRepository.saveAndFlush(newTask());

    assertThatThrownBy(() -> entityManager
        .createNativeQuery("UPDATE task SET status = 'RUNNING' WHERE id = :id")
        .setParameter("id", task.getId())
        .executeUpdate())
        .isInstanceOf(PersistenceException.class)
        .hasStackTraceContaining("ck_task_status");
  }
}
