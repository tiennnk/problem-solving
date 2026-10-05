package io.github.tiennnk.problemsolving.common.llm;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OpenAiCompatibleLlmClient implements LlmClient {

  private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleLlmClient.class);
  private static final double TEMPERATURE = 0.2;
  private static final Pattern RETRY_IN_SECONDS =
      Pattern.compile("retry in (\\d+(?:\\.\\d+)?)s", Pattern.CASE_INSENSITIVE);

  private final LlmProperties properties;
  private final RestClient restClient;

  public OpenAiCompatibleLlmClient(LlmProperties properties, RestClient.Builder restClientBuilder) {
    this.properties = properties;
    this.restClient = restClientBuilder
        .baseUrl(properties.baseUrl())
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build();
  }

  @Override
  public LlmResponse complete(LlmRequest request) {
    if (!properties.hasApiKey()) {
      throw new LlmException("LLM_API_KEY is not configured");
    }

    ChatCompletionRequest chatRequest = toChatRequest(request);
    int maxAttempts = properties.maxAttempts();
    for (int attempt = 1; ; attempt++) {
      try {
        return toLlmResponse(send(chatRequest));
      } catch (TransientLlmException ex) {
        if (attempt >= maxAttempts) {
          throw new LlmException("%s (gave up after %d attempts)".formatted(ex.getMessage(), attempt), ex);
        }
        Duration delay = retryDelay(attempt, ex);
        if (delay.compareTo(properties.maxRetryDelay()) > 0) {
          throw new LlmException("%s (provider asks to wait %d s, above the %d s limit)".formatted(
              ex.getMessage(), delay.toSeconds(), properties.maxRetryDelay().toSeconds()), ex);
        }
        log.warn("LLM call failed (attempt {}/{}): {}. Retrying in {} ms",
            attempt, maxAttempts, ex.getMessage(), delay.toMillis());
        sleep(delay);
      }
    }
  }

  private Duration retryDelay(int attempt, TransientLlmException ex) {
    Duration backoff = properties.retryBackoff().multipliedBy(1L << (attempt - 1));
    if (ex.retryAfter() == null || ex.retryAfter().compareTo(backoff) <= 0) {
      return backoff;
    }
    return ex.retryAfter();
  }

  private static Duration serverRetryAfter(RestClientResponseException ex) {
    HttpHeaders headers = ex.getResponseHeaders();
    String header = headers != null ? headers.getFirst(HttpHeaders.RETRY_AFTER) : null;
    if (header != null && header.strip().matches("\\d+")) {
      return Duration.ofSeconds(Long.parseLong(header.strip()));
    }
    Matcher matcher = RETRY_IN_SECONDS.matcher(ex.getResponseBodyAsString());
    if (matcher.find()) {
      double seconds = Double.parseDouble(matcher.group(1));
      return Duration.ofMillis((long) Math.ceil(seconds * 1000));
    }
    return null;
  }

  private ChatCompletionResponse send(ChatCompletionRequest chatRequest) {
    try {
      return restClient.post()
          .uri("/chat/completions")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
          .body(chatRequest)
          .retrieve()
          .body(ChatCompletionResponse.class);
    } catch (RestClientResponseException ex) {
      String message = "LLM provider returned HTTP %d: %s"
          .formatted(ex.getStatusCode().value(), abbreviate(ex.getResponseBodyAsString()));
      if (isTransient(ex.getStatusCode())) {
        throw new TransientLlmException(message, serverRetryAfter(ex), ex);
      }
      throw new LlmException(message, ex);
    } catch (ResourceAccessException ex) {
      throw new TransientLlmException("LLM provider is unreachable: " + ex.getMessage(), null, ex);
    } catch (RestClientException ex) {
      throw new LlmException("LLM provider returned an unexpected response: " + ex.getMessage(), ex);
    }
  }

  private static boolean isTransient(HttpStatusCode status) {
    return status.value() == 429 || status.is5xxServerError();
  }

  private static void sleep(Duration delay) {
    try {
      Thread.sleep(delay);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new LlmException("Interrupted while waiting to retry LLM call", ex);
    }
  }

  private ChatCompletionRequest toChatRequest(LlmRequest request) {
    List<Message> messages = new ArrayList<>();
    if (request.systemPrompt() != null && !request.systemPrompt().isBlank()) {
      messages.add(new Message("system", request.systemPrompt()));
    }
    messages.add(new Message("user", request.userPrompt()));

    ResponseFormat responseFormat = request.jsonOutput() ? new ResponseFormat("json_object") : null;
    return new ChatCompletionRequest(properties.model(), messages, TEMPERATURE, responseFormat);
  }

  private LlmResponse toLlmResponse(ChatCompletionResponse response) {
    if (response == null || response.choices() == null || response.choices().isEmpty()) {
      throw new LlmException("LLM provider returned no choices");
    }
    Message message = response.choices().getFirst().message();
    if (message == null || message.content() == null || message.content().isBlank()) {
      throw new LlmException("LLM provider returned an empty message");
    }

    TokenUsage usage = response.usage() == null
        ? TokenUsage.UNKNOWN
        : new TokenUsage(response.usage().promptTokens(), response.usage().completionTokens());
    String model = response.model() != null ? response.model() : properties.model();
    return new LlmResponse(message.content(), model, usage);
  }

  private static String abbreviate(String text) {
    if (text == null) {
      return "";
    }
    return text.length() <= 500 ? text : text.substring(0, 500) + "...";
  }

  private static class TransientLlmException extends RuntimeException {

    private final Duration retryAfter;

    TransientLlmException(String message, Duration retryAfter, Throwable cause) {
      super(message, cause);
      this.retryAfter = retryAfter;
    }

    Duration retryAfter() {
      return retryAfter;
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  record ChatCompletionRequest(
      String model,
      List<Message> messages,
      double temperature,
      @JsonProperty("response_format") ResponseFormat responseFormat) {
  }

  record Message(String role, String content) {
  }

  record ResponseFormat(String type) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record ChatCompletionResponse(String model, List<Choice> choices, Usage usage) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record Choice(Message message) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record Usage(
      @JsonProperty("prompt_tokens") int promptTokens,
      @JsonProperty("completion_tokens") int completionTokens) {
  }
}
