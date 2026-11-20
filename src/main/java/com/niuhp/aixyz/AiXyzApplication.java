package com.niuhp.aixyz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI 工单草稿应用入口。
 *
 * @author niuhaipeng
 */
@SpringBootApplication
public class AiXyzApplication {

    private static final String EVALUATION_ENABLED_PROPERTY = "app.evaluation.enabled";

    public static void main(String[] args) {
        var context = SpringApplication.run(AiXyzApplication.class, args);
        if (context.getEnvironment().getProperty(EVALUATION_ENABLED_PROPERTY, Boolean.class, false)) {
            context.close();
        }
    }
}
