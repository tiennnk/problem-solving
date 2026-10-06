package io.github.tiennnk.problemsolving.evaluation.api;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.tiennnk.problemsolving.evaluation.application.TestGenerator;
import io.github.tiennnk.problemsolving.evaluation.domain.GeneratedTestSuite;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Profile("dev")
@RestController
@RequestMapping("/api/dev/tests")
public class TestGeneratorPlaygroundController {

  private final TestGenerator testGenerator;

  public TestGeneratorPlaygroundController(TestGenerator testGenerator) {
    this.testGenerator = testGenerator;
  }

  @PostMapping("/generate")
  public GeneratedTestSuite generate(@Valid @RequestBody GenerateRequest request) {
    return testGenerator.generate(request.problem());
  }

  public record GenerateRequest(@NotBlank @Size(max = 20_000) String problem) {
  }
}
