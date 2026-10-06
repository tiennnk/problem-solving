package io.github.tiennnk.problemsolving.evaluation.api;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.tiennnk.problemsolving.evaluation.application.TestSuiteService;
import io.github.tiennnk.problemsolving.evaluation.domain.VerifiedTestSuite;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Profile("dev")
@RestController
@RequestMapping("/api/dev/tests")
public class TestGeneratorPlaygroundController {

  private final TestSuiteService testSuiteService;

  public TestGeneratorPlaygroundController(TestSuiteService testSuiteService) {
    this.testSuiteService = testSuiteService;
  }

  @PostMapping("/generate")
  public VerifiedTestSuite generate(@Valid @RequestBody GenerateRequest request) {
    return testSuiteService.prepare(request.problem());
  }

  public record GenerateRequest(@NotBlank @Size(max = 20_000) String problem) {
  }
}
