package com.niuhaipeng.aixyz.model;

import com.niuhaipeng.aixyz.service.ProcessingError;
import com.niuhaipeng.aixyz.service.TicketProcessingException;
import com.openai.errors.OpenAIIoException;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证 SDK 超时异常会转为安全的业务错误。
 *
 * @author niuhaipeng
 */
class SpringAiTicketModelGatewayTest {

    @Test
    void shouldMapSocketTimeoutWithoutExposingProviderDetails() {
        ChatModel model = prompt -> {
            throw new OpenAIIoException("sensitive-provider-detail", new SocketTimeoutException("private"));
        };
        SpringAiTicketModelGateway gateway = new SpringAiTicketModelGateway(
                ChatClient.create(model), "extract", JsonMapper.builder().build());
        assertThatThrownBy(() -> gateway.extract("导出失败"))
                .isInstanceOf(TicketProcessingException.class)
                .hasMessage(ProcessingError.MODEL_TIMEOUT.getMessage())
                .hasNoCause();
    }
}
