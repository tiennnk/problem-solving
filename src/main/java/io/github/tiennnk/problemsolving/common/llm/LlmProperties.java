package io.github.tiennnk.problemsolving.common.llm;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "llm")
public record LlmProperties(
    String baseUrl,
    String apiKey,
    String model,
    Duration timeout,
    int maxAttempts,
    Duration retryBackoff,
    Duration maxRetryDelay) {

  public LlmProperties {
    if (timeout == null) {
      timeout = Duration.ofSeconds(60);
    }
    if (maxAttempts <= 0) {
      maxAttempts = 3;
    }
    if (retryBackoff == null) {
      retryBackoff = Duration.ofSeconds(2);
    }
    if (maxRetryDelay == null) {
      maxRetryDelay = Duration.ofSeconds(65);
    }
  }

  public boolean hasApiKey() {
    return apiKey != null && !apiKey.isBlank();
  }
}
