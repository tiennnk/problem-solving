package io.github.tiennnk.problemsolving.evaluation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OutputNormalizerTest {

  @Test
  void ignoresLineEndingsTrailingSpacesAndTrailingBlankLines() {
    assertThat(OutputNormalizer.sameOutput("0 1  \r\n2 3\r\n\r\n", "0 1\n2 3")).isTrue();
  }

  @Test
  void keepsLeadingAndInnerWhitespaceSignificant() {
    assertThat(OutputNormalizer.sameOutput(" 0 1", "0 1")).isFalse();
    assertThat(OutputNormalizer.sameOutput("0  1", "0 1")).isFalse();
    assertThat(OutputNormalizer.sameOutput("0\n\n1", "0\n1")).isFalse();
  }
}
