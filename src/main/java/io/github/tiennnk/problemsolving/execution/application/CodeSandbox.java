package io.github.tiennnk.problemsolving.execution.application;

import io.github.tiennnk.problemsolving.common.domain.Language;

public interface CodeSandbox {

  SandboxSession start(Language language, String sourceCode);
}
