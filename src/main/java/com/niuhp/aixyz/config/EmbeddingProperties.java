package com.niuhp.aixyz.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 独立的向量模型配置，默认关闭，不复用聊天服务密钥。
 *
 * @author niuhaipeng
 */
@ConfigurationProperties(prefix = "app.knowledge.embedding")
public class EmbeddingProperties {

    private boolean enabled = false;

    private String baseUrl = "";

    private String apiKey = "";

    private String model = "";

    private Duration timeout = Duration.ofSeconds(15);

    private double threshold = 0.55;

    private String queryPrefix = "";

    private String documentPrefix = "";

    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public String getQueryPrefix() {
        return queryPrefix;
    }

    public void setQueryPrefix(String queryPrefix) {
        this.queryPrefix = queryPrefix;
    }

    public String getDocumentPrefix() {
        return documentPrefix;
    }

    public void setDocumentPrefix(String documentPrefix) {
        this.documentPrefix = documentPrefix;
    }

    @Override
    public String toString() {
        return "EmbeddingProperties{content=[REDACTED]}";
    }
}
