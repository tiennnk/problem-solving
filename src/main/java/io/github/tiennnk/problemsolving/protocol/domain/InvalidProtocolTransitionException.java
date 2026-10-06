package io.github.tiennnk.problemsolving.protocol.domain;

import java.util.UUID;

public class InvalidProtocolTransitionException extends RuntimeException {

  public InvalidProtocolTransitionException(UUID protocolRunId, ProblemSolverState from, ProblemSolverState to) {
    super("Protocol run %s cannot transition from %s to %s".formatted(protocolRunId, from, to));
  }
}
