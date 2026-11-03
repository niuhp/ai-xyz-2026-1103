# 验证记录

验证日期：2026-11-03。本记录用于复现教学演示，不代表模型正确率或性能承诺。

## 构建

- JDK：25.0.3。
- Spring Boot：4.0.8；Spring AI：2.0.1。
- 命令：`JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-25.0.3.jdk/Contents/Home mvn clean verify`。
- 结果：成功，11 个测试，0 失败，0 错误。
- Alibaba P3C：0 违规，0 解析错误，包含测试源码。
- 可共享源码及配置未发现真实 API Key；本地 `.env.local` 已被 Git 忽略，权限为 600。

## 真实模型调用

模型响应标识均为 `deepseek-v4.1-flash`。

| 输入 | HTTP / 状态 | 耗时 | 上游报告总 Token |
| --- | --- | --- | --- |
| 完整的字典重复值反馈 | 200 / READY | 5591 ms | 1246 |
| 昨天下午开始导出失败，提示超时，重新登录也没用 | 200 / NEEDS_INFO | 3744 ms | 994 |
| 帮我写一首关于秋天的诗 | 200 / UNSUPPORTED | 1935 ms | 754 |

第二例缺少系统名称、功能模块、预期结果和系统版本，时间字段保留“昨天下午”；第三例 draft 为 null。原始应用响应位于 Git 忽略的 `.local/live-*.json`。

最终打包版本还通过页面按钮完成一次真实调用，展示 READY 和提取字段，耗时 5393 ms、上游报告 1194 Token。页面截图保存于 `.local/preview.png` 和 `.local/preview-full.png`。

健康检查返回 200 / UP；错误方法返回 405，未知路径返回 404，数字类型 description 返回 400，均已用实际本地 HTTP 请求验证。

Token 是供应商报告的用量，不是费用；以上耗时与输出只代表单次观察。所有问题描述均为公开问题类型改写的教学输入，没有验证 RuoYi 对应版本存在该缺陷。
