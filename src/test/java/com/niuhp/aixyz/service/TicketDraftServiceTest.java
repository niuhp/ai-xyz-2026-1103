package com.niuhp.aixyz.service;

import com.niuhp.aixyz.model.ExtractionResult;
import com.niuhp.aixyz.model.ModelExtractionDTO;
import com.niuhp.aixyz.model.TicketModelGateway;
import com.niuhp.aixyz.repository.TicketRepository;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证并发额度保护和失败后的额度释放。
 *
 * @author niuhaipeng
 */
class TicketDraftServiceTest {

    private static final long WAIT_TIMEOUT_SECONDS = 5L;

    @Test
    void shouldRejectExcessConcurrencyAndReleasePermitAfterProviderFailure() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        TicketModelGateway gateway = description -> {
            entered.countDown();
            try {
                if (!release.await(WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("等待测试信号超时");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(exception);
            }
            throw new TicketProcessingException(ProcessingError.MODEL_UNAVAILABLE);
        };
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            TicketDraftService service = new TicketDraftService(gateway, factory.getValidator(),
                    org.mockito.Mockito.mock(TicketRepository.class), JsonMapper.builder().build(), 1);
            CompletableFuture<Void> first = CompletableFuture.runAsync(() ->
                    assertThatThrownBy(() -> service.generate("导出失败", "first"))
                            .isInstanceOf(TicketProcessingException.class)
                            .hasMessage(ProcessingError.MODEL_UNAVAILABLE.getMessage()));
            try {
                assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
                assertThatThrownBy(() -> service.generate("导出失败", "second"))
                        .isInstanceOf(TicketProcessingException.class)
                        .hasMessage(ProcessingError.BUSY.getMessage());
            } finally {
                release.countDown();
            }
            first.get(5, TimeUnit.SECONDS);
            assertThatThrownBy(() -> service.generate("导出失败", "third"))
                    .hasMessage(ProcessingError.MODEL_UNAVAILABLE.getMessage());
        }
    }

    @Test
    void shouldRejectInconsistentRelevanceFlag() {
        ModelExtractionDTO extraction = new ModelExtractionDTO();
        extraction.setRelevant(true);
        ExtractionResult result = new ExtractionResult();
        result.setExtraction(extraction);
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            TicketDraftService service = new TicketDraftService(description -> result, factory.getValidator(),
                    org.mockito.Mockito.mock(TicketRepository.class), JsonMapper.builder().build(), 1);
            assertThatThrownBy(() -> service.generate("导出失败", "test"))
                    .hasMessage(ProcessingError.INVALID_MODEL_OUTPUT.getMessage());
        }
    }
}
