package io.github.tiennnk.problemsolving.protocol.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.tiennnk.problemsolving.protocol.domain.ProtocolRun;

public interface ProtocolRunRepository extends JpaRepository<ProtocolRun, UUID> {

  List<ProtocolRun> findByTaskIdOrderByStartedAtDesc(UUID taskId);
}
