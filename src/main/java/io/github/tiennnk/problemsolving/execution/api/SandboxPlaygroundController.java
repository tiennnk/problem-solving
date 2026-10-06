package io.github.tiennnk.problemsolving.execution.api;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.tiennnk.problemsolving.common.domain.Language;
import io.github.tiennnk.problemsolving.execution.application.CodeSandbox;
import io.github.tiennnk.problemsolving.execution.application.SandboxSession;
import io.github.tiennnk.problemsolving.execution.domain.CompileResult;
import io.github.tiennnk.problemsolving.execution.domain.RunResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Profile("dev")
@RestController
@RequestMapping("/api/dev/sandbox")
public class SandboxPlaygroundController {

  private final CodeSandbox codeSandbox;

  public SandboxPlaygroundController(CodeSandbox codeSandbox) {
    this.codeSandbox = codeSandbox;
  }

  @PostMapping("/run")
  public RunResponse run(@Valid @RequestBody RunRequest request) {
    try (SandboxSession session = codeSandbox.start(request.language(), request.sourceCode())) {
      CompileResult compile = session.compile();
      if (!compile.success()) {
        return new RunResponse(compile, List.of());
      }
      List<RunResult> runs = request.inputs().stream().map(session::run).toList();
      return new RunResponse(compile, runs);
    }
  }

  public record RunRequest(
      @NotNull Language language,
      @NotBlank @Size(max = 50_000) String sourceCode,
      @NotNull @Size(min = 1, max = 20) List<@NotNull @Size(max = 5_000) String> inputs) {
  }

  public record RunResponse(CompileResult compile, List<RunResult> runs) {
  }
}
