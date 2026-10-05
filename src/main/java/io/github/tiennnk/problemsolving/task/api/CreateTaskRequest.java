package io.github.tiennnk.problemsolving.task.api;

import io.github.tiennnk.problemsolving.common.domain.Language;
import io.github.tiennnk.problemsolving.common.domain.ProtocolType;
import io.github.tiennnk.problemsolving.task.application.CreateTaskCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTaskRequest(
    @NotBlank @Size(max = 20_000) String problem,
    @NotNull Language language,
    ProtocolType protocolType) {

  CreateTaskCommand toCommand() {
    return new CreateTaskCommand(problem, language, protocolType);
  }
}
