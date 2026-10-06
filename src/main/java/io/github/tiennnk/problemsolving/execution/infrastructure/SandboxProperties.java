package io.github.tiennnk.problemsolving.execution.infrastructure;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sandbox")
public record SandboxProperties(
    String dockerCommand,
    String javaImage,
    String phpImage,
    int memoryMb,
    double cpus,
    int pidsLimit,
    Duration compileTimeout,
    Duration runTimeout,
    int maxOutputBytes) {

  public SandboxProperties {
    if (dockerCommand == null || dockerCommand.isBlank()) {
      dockerCommand = "docker";
    }
    if (javaImage == null || javaImage.isBlank()) {
      javaImage = "eclipse-temurin:21-jdk-alpine";
    }
    if (phpImage == null || phpImage.isBlank()) {
      phpImage = "php:8.4-cli-alpine";
    }
    if (memoryMb <= 0) {
      memoryMb = 512;
    }
    if (cpus <= 0) {
      cpus = 1.0;
    }
    if (pidsLimit <= 0) {
      pidsLimit = 128;
    }
    if (compileTimeout == null) {
      compileTimeout = Duration.ofSeconds(30);
    }
    if (runTimeout == null) {
      runTimeout = Duration.ofSeconds(5);
    }
    if (maxOutputBytes <= 0) {
      maxOutputBytes = 1_000_000;
    }
  }

  public static SandboxProperties defaults() {
    return new SandboxProperties(null, null, null, 0, 0, 0, null, null, 0);
  }
}
