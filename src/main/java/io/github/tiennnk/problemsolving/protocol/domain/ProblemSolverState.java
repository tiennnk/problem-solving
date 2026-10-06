package io.github.tiennnk.problemsolving.protocol.domain;

import java.util.Set;

public enum ProblemSolverState {
  ANALYZE,
  PLAN,
  DISPATCH,
  SOLVE,
  EXECUTE,
  EVALUATE,
  REPAIR,
  OPTIMIZE,
  REPORT,
  DONE,
  FAILED;

  public boolean isTerminal() {
    return this == DONE || this == FAILED;
  }

  public boolean canTransitionTo(ProblemSolverState target) {
    if (isTerminal()) {
      return false;
    }
    if (target == FAILED) {
      return true;
    }
    return nextStates().contains(target);
  }

  private Set<ProblemSolverState> nextStates() {
    return switch (this) {
      case ANALYZE -> Set.of(PLAN);
      case PLAN -> Set.of(DISPATCH);
      case DISPATCH -> Set.of(SOLVE);
      case SOLVE -> Set.of(EXECUTE);
      case EXECUTE -> Set.of(EVALUATE);
      case EVALUATE -> Set.of(OPTIMIZE, REPAIR);
      case REPAIR -> Set.of(EXECUTE);
      case OPTIMIZE -> Set.of(REPORT);
      case REPORT -> Set.of(DONE);
      case DONE, FAILED -> Set.of();
    };
  }
}
