package io.github.tiennnk.problemsolving.evaluation.domain;

import java.util.Arrays;
import java.util.stream.Collectors;

public final class OutputNormalizer {

  private OutputNormalizer() {
  }

  public static String normalize(String output) {
    if (output == null) {
      return "";
    }
    String unified = output.replace("\r\n", "\n").replace('\r', '\n');
    String joined = Arrays.stream(unified.split("\n", -1))
        .map(String::stripTrailing)
        .collect(Collectors.joining("\n"));
    return joined.replaceAll("\\n+$", "");
  }

  public static boolean sameOutput(String actual, String expected) {
    return normalize(actual).equals(normalize(expected));
  }
}
