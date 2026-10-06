package io.github.tiennnk.problemsolving.solution.infrastructure;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.tiennnk.problemsolving.solution.domain.Solution;

public interface SolutionRepository extends JpaRepository<Solution, UUID> {

  List<Solution> findByWorkerRunIdInOrderByCreatedAt(Collection<UUID> workerRunIds);
}
