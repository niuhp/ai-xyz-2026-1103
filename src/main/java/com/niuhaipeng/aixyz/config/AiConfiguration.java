package com.niuhaipeng.aixyz.config;

import java.nio.charset.StandardCharsets;
import java.io.IOException;
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
 * 统一配置模型调用：JSON 对象输出、超时和零自动重试。
 *
 * @author niuhaipeng
 */
@Configuration
public class AiConfiguration {

    @Bean
    public ChatClient ticketChatClient(ChatClient.Builder builder, OpenAiCommonProperties commonProperties,
            OpenAiChatProperties chatProperties) {
        OpenAiChatOptions.Builder options = OpenAiChatOptions.builder()
                .model(chatProperties.getModel())
                .temperature(0.0)
                .maxCompletionTokens(2048)
                .maxRetries(0)
                .timeout(OpenAiAutoConfigurationUtil.resolveCommonProperties(commonProperties, chatProperties)
                        .getTimeout())
                .responseFormat(ResponseFormat.builder().type(ResponseFormat.Type.JSON_OBJECT).build());
        return builder.defaultOptions(options).build();
    }

    @Bean
    public String ticketSystemPrompt() throws IOException {
        return new ClassPathResource("prompts/ticket-extraction.txt")
                .getContentAsString(StandardCharsets.UTF_8);
    }
}
