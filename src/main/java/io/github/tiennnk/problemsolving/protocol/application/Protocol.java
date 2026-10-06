package io.github.tiennnk.problemsolving.protocol.application;

import java.util.UUID;

import io.github.tiennnk.problemsolving.common.domain.ProtocolType;

public interface Protocol {

  ProtocolType type();

  void run(UUID taskId);
}
