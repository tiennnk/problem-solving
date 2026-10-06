package io.github.tiennnk.problemsolving.common.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public final class Timestamps {

  private Timestamps() {
  }

  public static Instant now() {
    return Instant.now().truncatedTo(ChronoUnit.MICROS);
  }
}
