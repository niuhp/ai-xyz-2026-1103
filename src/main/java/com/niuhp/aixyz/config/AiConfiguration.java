package com.niuhp.aixyz.config;

import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import okhttp3.Request;
import org.springframework.ai.openai.http.okhttp.OpenAiHttpClientBuilderCustomizer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.openai.autoconfigure.OpenAiAutoConfigurationUtil;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatProperties;
import org.springframework.ai.model.openai.autoconfigure.OpenAiCommonProperties;
import org.springframework.ai.openai.OpenAiChatModel.ResponseFormat;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

/**
 * 统一配置模型调用：JSON 输出、单次超时和零 SDK 重试；重试由应用层管理。
 *
 * @author niuhaipeng
 */
@Configuration
public class AiConfiguration {

    @Bean
    public OpenAiHttpClientBuilderCustomizer disableImplicitRequestReplay() {
        // OkHttp 对 503 + Retry-After: 0 也可能隐式重放 POST；SDK 重试为 0 还不够。
        return builder -> builder.interceptor(chain -> {
            Request request = chain.request();
            if (request.body() != null) {
                request = request.newBuilder().method(request.method(),
                        new OneShotModelRequestBody(request.body())).build();
            }
            return chain.proceed(request);
        });
    }

    @Bean
    @Primary
    public ChatClient ticketChatClient(ChatClient.Builder builder, OpenAiCommonProperties commonProperties,
            OpenAiChatProperties chatProperties) {
        return builder.defaultOptions(options(chatProperties,
                OpenAiAutoConfigurationUtil.resolveCommonProperties(commonProperties, chatProperties).getTimeout()))
                .build();
    }

    @Bean
    public ChatClient ticketStreamChatClient(ChatClient.Builder builder, OpenAiChatProperties chatProperties,
            @Value("${app.ticket.stream-timeout:45s}") Duration timeout) {
        return builder.defaultOptions(options(chatProperties, timeout).streamUsage(true)).build();
    }

    private OpenAiChatOptions.Builder options(OpenAiChatProperties chatProperties, Duration timeout) {
        return OpenAiChatOptions.builder()
                .model(chatProperties.getModel())
                .temperature(0.0)
                .maxCompletionTokens(2048)
                .maxRetries(0)
                .timeout(timeout)
                .responseFormat(ResponseFormat.builder().type(ResponseFormat.Type.JSON_OBJECT).build());
    }

    @Bean
    public String ticketSystemPrompt() throws IOException {
        return new ClassPathResource("prompts/ticket-extraction.txt")
                .getContentAsString(StandardCharsets.UTF_8);
    }
}
