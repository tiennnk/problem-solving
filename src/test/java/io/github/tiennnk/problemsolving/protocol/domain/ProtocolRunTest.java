package io.github.tiennnk.problemsolving.protocol.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.github.tiennnk.problemsolving.common.domain.ProtocolType;

class ProtocolRunTest {

  private static ProtocolRun newRun() {
    return ProtocolRun.start(UUID.randomUUID(), ProtocolType.PROBLEM_SOLVER);
  }

  private static ProtocolRun runAtEvaluate() {
    ProtocolRun run = newRun();
    run.transitionTo(ProblemSolverState.PLAN);
    run.transitionTo(ProblemSolverState.DISPATCH);
    run.transitionTo(ProblemSolverState.SOLVE);
    run.transitionTo(ProblemSolverState.EXECUTE);
    run.transitionTo(ProblemSolverState.EVALUATE);
    return run;
  }

  @Test
  void happyPathReachesDone() {
    ProtocolRun run = runAtEvaluate();

    run.transitionTo(ProblemSolverState.OPTIMIZE);
    run.transitionTo(ProblemSolverState.REPORT);
    run.transitionTo(ProblemSolverState.DONE);

    assertThat(run.getCurrentState()).isEqualTo(ProblemSolverState.DONE);
    assertThat(run.getCompletedAt()).isNotNull();
    assertThat(run.getAttempt()).isZero();
  }

  @Test
  void repairLoopsBackToExecuteAndCountsAttempts() {
    ProtocolRun run = runAtEvaluate();

    run.transitionTo(ProblemSolverState.REPAIR);
    run.transitionTo(ProblemSolverState.EXECUTE);
    run.transitionTo(ProblemSolverState.EVALUATE);
    run.transitionTo(ProblemSolverState.REPAIR);
    run.transitionTo(ProblemSolverState.EXECUTE);
    run.transitionTo(ProblemSolverState.EVALUATE);

    assertThat(run.getAttempt()).isEqualTo(2);
    assertThat(run.canRepair()).isFalse();
    assertThatThrownBy(() -> run.transitionTo(ProblemSolverState.REPAIR))
        .isInstanceOf(InvalidProtocolTransitionException.class);
  }

  @Test
  void rejectsSkippingStates() {
    ProtocolRun run = newRun();

    assertThatThrownBy(() -> run.transitionTo(ProblemSolverState.EXECUTE))
        .isInstanceOf(InvalidProtocolTransitionException.class)
        .hasMessageContaining("ANALYZE")
        .hasMessageContaining("EXECUTE");
    assertThat(run.getCurrentState()).isEqualTo(ProblemSolverState.ANALYZE);
  }

  @Test
  void cannotOptimizeBeforeEvaluate() {
    ProtocolRun run = newRun();
    run.transitionTo(ProblemSolverState.PLAN);
    run.transitionTo(ProblemSolverState.DISPATCH);
    run.transitionTo(ProblemSolverState.SOLVE);

    assertThatThrownBy(() -> run.transitionTo(ProblemSolverState.OPTIMIZE))
        .isInstanceOf(InvalidProtocolTransitionException.class);
  }

  @ParameterizedTest
  @EnumSource(value = ProblemSolverState.class, names = {"ANALYZE", "SOLVE", "EVALUATE"})
  void canFailFromActiveStates(ProblemSolverState state) {
    ProtocolRun run = newRun();
    for (ProblemSolverState step : new ProblemSolverState[] {
        ProblemSolverState.PLAN, ProblemSolverState.DISPATCH, ProblemSolverState.SOLVE,
        ProblemSolverState.EXECUTE, ProblemSolverState.EVALUATE}) {
      if (run.getCurrentState() == state) {
        break;
      }
      run.transitionTo(step);
    }

    run.fail("boom");

    assertThat(run.getCurrentState()).isEqualTo(ProblemSolverState.FAILED);
    assertThat(run.getFailureDetail()).isEqualTo("boom");
  }

  @Test
  void terminalRunAcceptsNoTransition() {
    ProtocolRun run = newRun();
    run.fail("analyzer down");

    assertThatThrownBy(() -> run.transitionTo(ProblemSolverState.PLAN))
        .isInstanceOf(InvalidProtocolTransitionException.class);
    assertThatThrownBy(() -> run.fail("again"))
        .isInstanceOf(InvalidProtocolTransitionException.class);
  }
}
