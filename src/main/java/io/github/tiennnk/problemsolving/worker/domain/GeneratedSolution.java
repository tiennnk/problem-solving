package io.github.tiennnk.problemsolving.worker.domain;

import io.github.tiennnk.problemsolving.common.domain.Language;
import io.github.tiennnk.problemsolving.common.llm.TokenUsage;

public record GeneratedSolution(
    Language language,
    String sourceCode,
    String claimedTimeComplexity,
    String claimedSpaceComplexity,
    String explanation,
    String model,
    TokenUsage tokenUsage) {
}
