package io.github.tiennnk.problemsolving.common.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenAiCompatibleLlmClientTest {

  private static final String BASE_URL = "https://llm.example.com/v1";

  private MockRestServiceServer server;
  private OpenAiCompatibleLlmClient client;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder();
    server = MockRestServiceServer.bindTo(builder).build();
    LlmProperties properties = new LlmProperties(
        BASE_URL, "test-key", "test-model", Duration.ofSeconds(5), 3, Duration.ZERO, Duration.ofSeconds(1));
    client = new OpenAiCompatibleLlmClient(properties, builder);
  }

  @Test
  void sendsChatRequestAndParsesResponse() {
    server.expect(requestTo(BASE_URL + "/chat/completions"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer test-key"))
        .andExpect(jsonPath("$.model").value("test-model"))
        .andExpect(jsonPath("$.messages[0].role").value("system"))
        .andExpect(jsonPath("$.messages[1].content").value("Say hi"))
        .andExpect(jsonPath("$.response_format.type").value("json_object"))
        .andRespond(withSuccess("""
            {
              "model": "test-model-2025",
              "choices": [{"index": 0, "message": {"role": "assistant", "content": "{\\"answer\\":\\"hi\\"}"}, "finish_reason": "stop"}],
              "usage": {"prompt_tokens": 12, "completion_tokens": 5, "total_tokens": 17}
            }
            """, MediaType.APPLICATION_JSON));

    LlmResponse response = client.complete(LlmRequest.json("You are helpful", "Say hi"));

    assertThat(response.content()).isEqualTo("{\"answer\":\"hi\"}");
    assertThat(response.model()).isEqualTo("test-model-2025");
    assertThat(response.usage()).isEqualTo(new TokenUsage(12, 5));
    server.verify();
  }

  @Test
  void wrapsNonJsonResponses() {
    server.expect(requestTo(BASE_URL + "/chat/completions"))
        .andRespond(withSuccess("OK", MediaType.TEXT_PLAIN));

    assertThatThrownBy(() -> client.complete(LlmRequest.text(null, "Say hi")))
        .isInstanceOf(LlmException.class)
        .hasMessageContaining("unexpected response");
  }

  @Test
  void retriesTransientErrorsThenSucceeds() {
    server.expect(requestTo(BASE_URL + "/chat/completions"))
        .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE).body("high demand"));
    server.expect(requestTo(BASE_URL + "/chat/completions"))
        .andRespond(withSuccess("""
            {"choices": [{"message": {"role": "assistant", "content": "hi"}}]}
            """, MediaType.APPLICATION_JSON));

    LlmResponse response = client.complete(LlmRequest.text(null, "Say hi"));

    assertThat(response.content()).isEqualTo("hi");
    server.verify();
  }

  @Test
  void givesUpAfterMaxAttempts() {
    server.expect(ExpectedCount.times(3), requestTo(BASE_URL + "/chat/completions"))
        .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("rate limited"));

    assertThatThrownBy(() -> client.complete(LlmRequest.text(null, "Say hi")))
        .isInstanceOf(LlmException.class)
        .hasMessageContaining("429")
        .hasMessageContaining("rate limited")
        .hasMessageContaining("after 3 attempts");
    server.verify();
  }

  @Test
  void waitsForServerSuggestedDelayThenSucceeds() {
    server.expect(requestTo(BASE_URL + "/chat/completions"))
        .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("Quota exceeded. Please retry in 0.05s."));
    server.expect(requestTo(BASE_URL + "/chat/completions"))
        .andRespond(withSuccess("""
            {"choices": [{"message": {"role": "assistant", "content": "hi"}}]}
            """, MediaType.APPLICATION_JSON));

    assertThat(client.complete(LlmRequest.text(null, "Say hi")).content()).isEqualTo("hi");
    server.verify();
  }

  @Test
  void givesUpImmediatelyWhenServerAsksToWaitTooLong() {
    server.expect(ExpectedCount.once(), requestTo(BASE_URL + "/chat/completions"))
        .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("Quota exceeded. Please retry in 58.4s."));

    assertThatThrownBy(() -> client.complete(LlmRequest.text(null, "Say hi")))
        .isInstanceOf(LlmException.class)
        .hasMessageContaining("asks to wait 58 s");
    server.verify();
  }

  @Test
  void doesNotRetryClientErrors() {
    server.expect(ExpectedCount.once(), requestTo(BASE_URL + "/chat/completions"))
        .andRespond(withStatus(HttpStatus.NOT_FOUND).body("model not found"));

    assertThatThrownBy(() -> client.complete(LlmRequest.text(null, "Say hi")))
        .isInstanceOf(LlmException.class)
        .hasMessageContaining("404");
    server.verify();
  }
}
