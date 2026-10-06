package io.github.tiennnk.problemsolving.worker.application;

import io.github.tiennnk.problemsolving.worker.domain.CodeGenerationRequest;
import io.github.tiennnk.problemsolving.worker.domain.GeneratedSolution;

public interface CodingWorker {

  GeneratedSolution generate(CodeGenerationRequest request);
}
