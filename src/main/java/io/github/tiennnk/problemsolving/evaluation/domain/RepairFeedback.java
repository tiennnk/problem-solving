package io.github.tiennnk.problemsolving.evaluation.domain;

public final class RepairFeedback {

  private RepairFeedback() {
  }

  public static String from(EvaluationResult result) {
    if (result.passed()) {
      return "";
    }
    if (result.verdict() == Verdict.COMPILE_ERROR) {
      return "The program does not compile. Compiler output:\n" + result.compilerOutput();
    }
    TestOutcome failure = result.firstFailure().orElseThrow();
    StringBuilder feedback = new StringBuilder()
        .append("Verdict: ").append(failure.verdict())
        .append(" on test #").append(failure.testNumber())
        .append(" (").append(result.passedTests()).append('/').append(result.totalTests()).append(" tests passed).\n")
        .append("Input:\n").append(failure.input()).append('\n');
    switch (failure.verdict()) {
      case WRONG_ANSWER -> feedback
          .append("Expected output:\n").append(failure.expectedOutput()).append('\n')
          .append("Your output:\n").append(failure.actualOutput()).append('\n');
      case RUNTIME_ERROR, RESOURCE_LIMIT -> feedback
          .append("Error output:\n").append(failure.stderr()).append('\n');
      case TIMEOUT -> feedback
          .append("The program exceeded the time limit after ").append(failure.runtimeMs()).append(" ms.\n");
      default -> {
      }
    }
    return feedback.toString();
  }
}
