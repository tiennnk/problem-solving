package io.github.tiennnk.problemsolving.common.llm;

import java.net.http.HttpClient;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(LlmProperties.class)
public class LlmConfiguration {

  @Bean
  LlmClient llmClient(LlmProperties properties) {
    HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(properties.timeout())
        .build();
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(properties.timeout());

    return new OpenAiCompatibleLlmClient(properties, RestClient.builder().requestFactory(requestFactory));
  }
}
