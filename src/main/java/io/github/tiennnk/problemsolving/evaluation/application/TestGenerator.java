package io.github.tiennnk.problemsolving.evaluation.application;

import io.github.tiennnk.problemsolving.evaluation.domain.GeneratedTestSuite;

public interface TestGenerator {

  GeneratedTestSuite generate(String problem);
}
