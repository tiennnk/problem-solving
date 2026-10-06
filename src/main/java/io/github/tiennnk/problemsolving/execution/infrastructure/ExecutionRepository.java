package io.github.tiennnk.problemsolving.execution.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.tiennnk.problemsolving.execution.domain.Execution;

public interface ExecutionRepository extends JpaRepository<Execution, UUID> {

  List<Execution> findBySolutionId(UUID solutionId);
}
