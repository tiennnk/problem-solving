package io.github.tiennnk.problemsolving.evaluation.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.tiennnk.problemsolving.evaluation.domain.TestCase;

public interface TestCaseRepository extends JpaRepository<TestCase, UUID> {

  List<TestCase> findByTaskIdOrderByPosition(UUID taskId);
}
