package com.niuhp.aixyz.api;

import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 仅本地演示模式启用故障选择器，正常启动不提供此入口。
 *
 * @author niuhaipeng
 */
@RestController
@Profile("demo")
@RequestMapping("/api/v1/demo")
public class DemoController {

    @GetMapping
    public Map<String, Boolean> configuration() {
        return Map.of("enabled", true);
    }
}
