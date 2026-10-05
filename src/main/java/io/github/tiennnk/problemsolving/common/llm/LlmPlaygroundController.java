package io.github.tiennnk.problemsolving.common.llm;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Profile("dev")
@RestController
@RequestMapping("/api/dev/llm")
public class LlmPlaygroundController {

  private final LlmClient llmClient;

  public LlmPlaygroundController(LlmClient llmClient) {
    this.llmClient = llmClient;
  }

  @PostMapping("/ask")
  public LlmResponse ask(@Valid @RequestBody AskRequest request) {
    return llmClient.complete(LlmRequest.text(null, request.prompt()));
  }

  public record AskRequest(@NotBlank @Size(max = 2_000) String prompt) {
  }
}
