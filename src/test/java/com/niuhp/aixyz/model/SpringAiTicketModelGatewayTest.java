package com.niuhp.aixyz.model;

import com.niuhp.aixyz.service.ProcessingError;
import com.niuhp.aixyz.service.TicketProcessingException;
import com.openai.errors.OpenAIIoException;
import java.net.SocketTimeoutException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

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
                ChatClient.create(model), "extract", JsonMapper.builder().build(),
                new ModelCallExecutor(new ModelFailureClassifier(), 1));
        assertThatThrownBy(() -> gateway.extract("导出失败"))
                .isInstanceOf(TicketProcessingException.class)
                .hasMessage(ProcessingError.MODEL_TIMEOUT.getMessage())
                .hasNoCause();
    }
    @Test
    void shouldNotRepeatUnknownNetworkFailure() {
        AtomicInteger attempts = new AtomicInteger();
        ModelCallExecutor executor = new ModelCallExecutor(new ModelFailureClassifier(), 1);
        assertThatThrownBy(() -> executor.call(() -> {
            attempts.incrementAndGet();
            throw new OpenAIIoException("sensitive-provider-detail", new IOException("private"));
        })).isInstanceOf(TicketProcessingException.class)
                .hasMessage(ProcessingError.MODEL_NETWORK_ERROR.getMessage()).hasNoCause();
        assertThat(attempts.get()).isEqualTo(1);
    }
    @Test
    void shouldRecognizePackagedHttpClientTimeout() {
        ModelCallExecutor executor = new ModelCallExecutor(new ModelFailureClassifier(), 1);
        assertThatThrownBy(() -> executor.call(() -> {
            throw new OpenAIIoException("sensitive-provider-detail", new InterruptedIOException("timeout"));
        })).isInstanceOf(TicketProcessingException.class)
                .hasMessage(ProcessingError.MODEL_TIMEOUT.getMessage()).hasNoCause();
    }
}
