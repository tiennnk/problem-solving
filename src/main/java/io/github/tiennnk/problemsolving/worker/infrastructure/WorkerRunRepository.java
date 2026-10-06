package io.github.tiennnk.problemsolving.worker.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.tiennnk.problemsolving.worker.domain.WorkerRun;

public interface WorkerRunRepository extends JpaRepository<WorkerRun, UUID> {

  List<WorkerRun> findByProtocolRunId(UUID protocolRunId);
}
