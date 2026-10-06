package io.github.tiennnk.problemsolving.worker.api;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.tiennnk.problemsolving.common.domain.Language;
import io.github.tiennnk.problemsolving.worker.application.CodingWorker;
import io.github.tiennnk.problemsolving.worker.domain.CodeGenerationRequest;
import io.github.tiennnk.problemsolving.worker.domain.GeneratedSolution;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Profile("dev")
@RestController
@RequestMapping("/api/dev/workers")
public class WorkerPlaygroundController {

  private final CodingWorker codingWorker;

  public WorkerPlaygroundController(CodingWorker codingWorker) {
    this.codingWorker = codingWorker;
  }

  @PostMapping("/solve")
  public GeneratedSolution solve(@Valid @RequestBody SolveRequest request) {
    return codingWorker.generate(new CodeGenerationRequest(
        request.problem(), request.language(), request.ioSpecification(), null));
  }

  public record SolveRequest(
      @NotBlank @Size(max = 20_000) String problem,
      @NotNull Language language,
      @Size(max = 2_000) String ioSpecification) {
  }
}
