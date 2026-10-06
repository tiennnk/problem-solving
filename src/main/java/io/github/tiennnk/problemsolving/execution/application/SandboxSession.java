package io.github.tiennnk.problemsolving.execution.application;

import io.github.tiennnk.problemsolving.execution.domain.CompileResult;
import io.github.tiennnk.problemsolving.execution.domain.RunResult;

public interface SandboxSession extends AutoCloseable {

  CompileResult compile();

  RunResult run(String stdin);

  @Override
  void close();
}
