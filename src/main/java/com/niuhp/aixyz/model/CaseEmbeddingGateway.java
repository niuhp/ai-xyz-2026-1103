package com.niuhp.aixyz.model;

import com.niuhp.aixyz.config.EmbeddingProperties;
import com.niuhp.aixyz.exception.ProcessingError;
import com.niuhp.aixyz.exception.TicketProcessingException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.http.okhttp.OpenAiHttpClientBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 向量模型适配器：批量结果按 index 归位，禁用自动重试并共享并发限额。
 *
 * @author niuhaipeng
 */
@Component
@EnableConfigurationProperties(EmbeddingProperties.class)
public class CaseEmbeddingGateway {

    private final OpenAiEmbeddingModel model;
    private final ModelConcurrencyLimiter limiter;

    public CaseEmbeddingGateway(EmbeddingProperties properties, ModelConcurrencyLimiter limiter,
            OpenAiHttpClientBuilderCustomizer customizer) {
        this.limiter = limiter;
        if (!properties.getEnabled()) {
            model = null;
            return;
        }
        Assert.isTrue(StringUtils.hasText(properties.getBaseUrl()) && StringUtils.hasText(properties.getApiKey())
                && StringUtils.hasText(properties.getModel()), "启用语义检索前请配置独立的 Embedding 地址、密钥和模型");
        Assert.isTrue(!properties.getTimeout().isNegative() && !properties.getTimeout().isZero(),
                "Embedding 超时必须大于零");
        model = OpenAiEmbeddingModel.builder().httpClientBuilderCustomizer(customizer)
                .options(OpenAiEmbeddingOptions.builder().baseUrl(properties.getBaseUrl())
                        .apiKey(properties.getApiKey()).model(properties.getModel())
                        .encodingFormat(OpenAiEmbeddingOptions.EncodingFormat.FLOAT)
                        .timeout(properties.getTimeout()).maxRetries(0).build()).build();
    }

    public List<float[]> embed(List<String> texts) {
        if (model == null) {
            throw new TicketProcessingException(ProcessingError.EMBEDDING_DISABLED);
        }
        limiter.acquirePermit();
        try {
            List<Embedding> results = model.call(new EmbeddingRequest(texts, null)).getResults();
            float[][] ordered = new float[texts.size()][];
            for (Embedding result : results) {
                int index = result.getIndex();
                if (index < 0 || index >= ordered.length || ordered[index] != null) {
                    throw new TicketProcessingException(ProcessingError.INVALID_EMBEDDING_OUTPUT);
                }
                ordered[index] = result.getOutput();
            }
            if (results.size() != texts.size() || Arrays.stream(ordered).anyMatch(Objects::isNull)) {
                throw new TicketProcessingException(ProcessingError.INVALID_EMBEDDING_OUTPUT);
            }
            return List.of(ordered);
        } catch (TicketProcessingException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            // 不对外暴露上游响应、密钥或文本；费用不确定的请求不自动重试。
            throw new TicketProcessingException(ProcessingError.EMBEDDING_UNAVAILABLE);
        } finally {
            limiter.releasePermit();
        }
    }
}
