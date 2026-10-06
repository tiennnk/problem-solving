package io.github.tiennnk.problemsolving.evaluation.infrastructure;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.tiennnk.problemsolving.evaluation.domain.Evaluation;

public interface EvaluationRepository extends JpaRepository<Evaluation, UUID> {

  Optional<Evaluation> findBySolutionId(UUID solutionId);

  List<Evaluation> findBySolutionIdIn(Collection<UUID> solutionIds);
}
