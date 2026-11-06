# ai-xyz-2026-1103

一个可直接运行的 Java AI 实战：**把用户的问题描述整理成工单草稿，人工确认后再创建工单**。

技术栈：JDK **25**、Spring Boot **4.0.8**、Spring AI **2.0.1**。默认使用 FastAI 的 `deepseek-v4.1-flash`，提供 REST API 和无需安装前端工具的演示页面；H2 文件数据库保存草稿与本地演示工单。

## 快速运行

需要 JDK 25、Maven 3.9.x。macOS 可用：

```bash
cd /Users/niuhaipeng/code-niuhp/ai-xyz-2026-1103
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
export PATH="$JAVA_HOME/bin:$PATH"
```

本机已配置 `.env.local`，不要覆盖；新环境使用下面的模板：

```bash
cp .env.example .env.local
chmod 600 .env.local
# 编辑 .env.local，将 FASTAI_API_KEY 替换为自己的密钥
```

配置项：

| 配置 | 默认值 / 说明 |
| --- | --- |
| `FASTAI_BASE_URL` | `https://www.fastaitoken.com/v1` |
| `FASTAI_MODEL` | `deepseek-v4.1-flash` |
| `FASTAI_API_KEY` | 在本地文件或环境变量中设置，无默认密钥 |
| `SERVER_PORT` | `8081` |
| `MODEL_TIMEOUT` | `15s`，单次 SDK 调用超时，不是整个接口总时限 |
| `MODEL_MAX_RETRIES` | `1`，仅允许 0 或 1 次额外重试 |

Spring AI 2 使用完整 API 基础地址，因此这里包含 `/v1`，实际请求为 `POST /v1/chat/completions`。不要再拼接第二个 `/v1`。

启动：

```bash
./scripts/run.sh
```

也可以在 IDE 中选择 JDK 25，**工作目录设为项目根目录**，运行 `AiXyzApplication`。IDE 的 Maven JDK 也应选择 25。

- 演示页面：[http://localhost:8081/](http://localhost:8081/)
- 健康检查：[http://localhost:8081/actuator/health](http://localhost:8081/actuator/health)
- 停止服务：运行终端中按 Ctrl+C。

`scripts/run.sh` 会先打包；已经构建后也可以直接运行：

```bash
java -jar target/ai-xyz-2026-1103-0.0.1-SNAPSHOT.jar
```

## 接口示例

```bash
curl -sS http://localhost:8081/api/v1/ticket-drafts \
  -H 'Content-Type: application/json' \
  -d '{"description":"我使用 RuoYi-Vue 3.8.9，在字典管理给同一字典类型添加相同字典值，两次都提示成功，希望重复添加时提示字典值已经存在。"}'
```

接口请求只有一个字段 `description`：不能为空、不能全为空白，最多 4000 个 Java 字符。更多示例见 [examples/requests.http](examples/requests.http)。

草稿状态为 `READY` 或 `NEEDS_INFO` 时可编辑字段。`draftId` 用于确认接口：

```bash
curl -sS http://localhost:8081/api/v1/ticket-drafts/confirm \
  -H 'Content-Type: application/json' \
  -d '{"draftId":"上一步返回的草稿编号","draft":{"system":"RuoYi-Vue","module":"字典管理","category":"DATA_DUPLICATION","summary":"重复字典值","actualResult":"重复值添加成功","expectedResult":"提示已存在","occurredAtText":"","version":"3.8.9"}}'
```

接口成功返回 `ticketId` 和 `CREATED`。同一个草稿编号重复提交会返回原工单编号，工单表上的唯一约束也会阻止重复创建。数据位于 `.local/ticketdb`，只用于本地演示。

响应示意（摘要及措辞可能因模型输出不同而变化）：

```json
{
  "requestId": "服务端生成的请求编号",
  "status": "READY",
  "draft": {
    "system": "RuoYi-Vue",
    "module": "字典管理",
    "category": "DATA_DUPLICATION",
    "summary": "相同字典值可以重复添加",
    "actualResult": "两次添加都提示成功",
    "expectedResult": "重复添加时提示字典值已经存在",
    "occurredAtText": "",
    "version": "3.8.9"
  },
  "missingFields": [],
  "usage": {
    "inputTokens": 773,
    "outputTokens": 473,
    "totalTokens": 1246
  },
  "model": "deepseek-v4.1-flash",
  "elapsedMillis": 5591
}
```

`usage` 来自上游响应，用量未知时为 null；不是收费金额，也不替代平台账单。示例数字取自一次实际调用，不代表每次调用的用量或速度。

### 状态与字段规则

| status | 含义 |
| --- | --- |
| `READY` | 必要字段已具备，仍需人工核对内容；没有创建真实工单 |
| `NEEDS_INFO` | 是软件反馈，但存在缺失项；查看 `missingFields` |
| `UNSUPPORTED` | 与软件反馈无关，`draft=null`、`missingFields=[]` |

Java 根据 **系统名称、功能模块、实际现象、预期结果、系统版本** 五项计算完整性，不采信模型提供的“已经完整”判断。模型响应本身没有 status 和 missingFields 字段。

- 未提供的文本字段使用空字符串，不填“未知版本”假装完整。
- 时间是可选信息；“昨天下午”保留原文，不推算成具体日期。
- 预期结果必须是用户明确表达的内容，不自动补写“应该操作成功”。
- 问题分类只有 `DATA_DUPLICATION / QUERY / EXPORT / ACCESS / OTHER`，用于整理问题类型。
- 描述真实与否、分类是否准确，仍需人工确认；Bean Validation 能校验结构，不能证明事实正确。

例如“昨天下午开始导出失败，提示超时，重新登录也没用”应返回 `NEEDS_INFO`；“帮我写一首诗”应返回 `UNSUPPORTED`。页面上的示例按钮只填入文本，点击“生成草稿”才调用模型。

### 错误响应

```json
{
  "requestId": "服务端生成的请求编号",
  "code": "INVALID_MODEL_OUTPUT",
  "message": "模型返回内容不符合工单结构，请调整描述后重试",
  "action": "请检查输入；仍有问题时提供请求编号给维护者。"
}
```

| HTTP | code | 场景 |
| --- | --- | --- |
| 400 | `INVALID_REQUEST` | 空白、超长或非法请求 JSON |
| 404 / 405 / 415 | `NOT_FOUND / METHOD_NOT_ALLOWED / UNSUPPORTED_MEDIA_TYPE` | 路径、方法、内容类型错误 |
| 502 | `INVALID_MODEL_OUTPUT` | 非法 JSON、未知字段或枚举、字段约束不满足、输出被截断 |
| 502 | `MODEL_UNAVAILABLE` | 上游服务故障，符合策略时已尝试一次重试 |
| 502 | `MODEL_AUTH_ERROR` | 上游 401/403，维护者检查密钥与权限 |
| 502 | `MODEL_QUOTA_EXHAUSTED` | 上游 402 或明确的额度不足错误码 |
| 503 | `MODEL_RATE_LIMITED` | 上游 429，且没有命中已知额度错误码 |
| 502 | `MODEL_REQUEST_REJECTED` | 上游 400/404/422，检查请求、模型和接口配置 |
| 502 | `MODEL_NETWORK_ERROR` | 网络调用失败，结果不确定，不自动重试 |
| 503 | `BUSY` | 本地已有两个模型请求在处理 |
| 504 | `MODEL_TIMEOUT` | 模型网络响应超时 |
| 500 | `INTERNAL_ERROR` | 其他未预期错误 |

响应头 `X-Request-ID` 与响应体 requestId 一致，可据此查应用日志。对外错误不带供应商原始报错、API Key 或用户描述。

## 调用链路

```mermaid
flowchart LR
    A[问题描述] --> B[Java 校验输入]
    B --> C[Spring AI 调用模型]
    C --> D[严格 JSON 解析]
    D --> E[Bean Validation]
    E --> F[Java 计算缺失项与状态]
    F --> G[人工审核草稿]
```

- 提示词放在 [ticket-extraction.txt](src/main/resources/prompts/ticket-extraction.txt)，可以独立阅读和修改。
- 固定规则放入 SystemMessage，描述放入 UserMessage；通过消息对象传递，输入里的花括号不会被模板引擎解释。
- 使用 JSON Object 输出模式，配合提示词中的 JSON Schema；供应商不强制执行业务规则，服务端仍必须解析、校验。
- 只接受正常结束的响应；即使返回的文本看似合法 JSON，完成原因为 length 也会拒绝。
- 严格拒绝未知 JSON 字段、枚举错误、多余尾部文本和标量类型强制转换。
- 单次 SDK 调用超时默认 15 秒，SDK 自动重试为 0；由应用集中控制是否额外调用，详见下节。
- 同时最多处理两个模型请求，满额立即返回 503，不无限排队。
- 无工具调用、无自动外部操作；提示词中的隔离规则可降低指令干扰，但不能保证模型完全不会编造。

主要源码：

```text
src/main/java/com/niuhp/aixyz/
├── AiXyzApplication.java
├── api/                  # Controller、请求/响应 DTO、请求编号、异常处理
├── config/               # 模型参数及提示词加载
├── domain/               # 工单字段及枚举
├── model/                # 模型适配器、严格 JSON 解析、用量转换
└── service/              # 业务校验、完整性计算、并发控制
src/main/resources/
├── application.yml
├── prompts/              # 系统提示词
└── static/               # 原生 HTML/CSS/JS 演示页面
```

## 第三篇：模型故障兜底

`ModelFailureClassifier` 负责分类，`ModelCallExecutor` 集中控制调用次数，`GlobalExceptionHandler` 返回稳定的 `code / message / action / requestId`。上表 HTTP 是本应用状态，不等于上游状态；上游密钥错误不会被当成当前用户登录失效。

- SDK 重试为 0，同时把 HTTP 请求体标为不可隐式重放，阻止传输层对 `503 + Retry-After: 0` 等情况额外发送 POST。
- **只对上游 HTTP 502/503 最多重试一次**，一次逻辑请求最多两次模型调用。其他 5xx 不自动重试。
- 无 `Retry-After` 时随机等待 250～500 毫秒；若该头存在，兼容秒数和 HTTP 日期，并至少等待其指定时间。无效、已过期或要求等待超过 2 秒时，本次不重试。
- 超时、未知网络错误、429、鉴权、额度、参数错误和非法输出均不自动重试。429 先识别已知额度错误码，供应商错误结构有差异，应按接入文档扩展。
- 默认 15 秒是**单次 SDK 调用超时**，额外调用会增加总耗时；这里没有实现端到端总时限、熔断或跨实例限流。
- 超时不等于上游没有执行，也不保证停止生成或计费。手动重试和自动重试都可能产生额外费用，以供应商账单为准。
- 重试只包住模型调用，JSON/业务校验和工单落库不在重试范围。模型失败时保留网页中的输入，不生成替代内容，不进入工单创建。
- 应用日志记录请求编号、尝试序号、结果码、耗时和重试等待，不记录密钥、用户描述或供应商报错原文。

### 不花模型额度的本地故障演示

需要 Python 3（仅标准库）：

```bash
./scripts/run-demo.sh
```

打开 [http://localhost:8082/?demo=1](http://localhost:8082/?demo=1)，选择超时、限流、鉴权失败、额度不足、503 后恢复或持续不可用等场景，再点“生成草稿”。终端显示每次模拟上游调用的序号，可核对实际尝试次数。

脚本显式使用 `demo` 配置、本机模拟地址和占位 Key，并禁用 `.env.local` 导入，不向真实供应商转发请求；数据库为独立内存库，端口默认 8082 / 18083。页面的故障选项只在 demo 配置下启用。终端按 **Ctrl+C** 同时停止应用和模拟服务，不影响另外运行的 8081。

IDEA 中正常使用：Project SDK、运行配置 JRE、Maven Runner JRE 均选 **25**；工作目录为项目根目录，运行 `AiXyzApplication`。正常调用会读取 `.env.local`，默认访问 8081。

如需在 IDEA 调试故障演示，先在终端运行 `python3 scripts/mock-model.py --port 18083`；运行配置的 Program arguments 填 `--spring.profiles.active=demo --spring.config.import=`，工作目录仍为项目根目录。访问 8082 的 `?demo=1` 页面，结束后分别停止 IDEA 应用和 Python 模拟服务。

## 测试与阿里规范检查

```bash
mvn clean verify
```

测试通过本地假 OpenAI HTTP 服务验证真实 Spring AI 请求格式、三个业务状态、输入限制、非法/截断输出、上游错误分类与脱敏、有界重试和真实 HTTP 超时；另有并发额度释放、异常关联结构和超时映射测试。自动化测试使用测试占位 Key，不访问真实供应商，不消耗模型额度。

`verify` 同时运行 Alibaba P3C PMD 检查，包含测试源码。报告在 `target/pmd.xml`。

P3C 2.1.1 基于 PMD 6，因此解析语法设为 Java 17；源码采用其可解析的语法，**应用编译与运行仍严格使用 JDK 25**，Enforcer 会拒绝其他 JDK。命名、常量、注释、集合和异常处理等遵循阿里规范；自动检查覆盖规则范围有限，不能替代人工审查。DTO 的 toString 对描述内容作隐藏处理。

项目通过 `.mvn/maven.config` 使用 [Maven Central](https://repo.maven.apache.org/maven2) 的独立 settings，避免本机公司 Nexus 配置影响依赖解析。企业网络需要专用镜像时，可调整 `.mvn/settings.xml`。

## 演示数据与文章素材

字典重复值案例参考 [RuoYi-Vue 公开 issue #196](https://github.com/yangzongzhuan/RuoYi-Vue/issues/196) 的问题类型，**已经改写为教学输入**；示例中的版本等字段为演示所加，不表示该版本已经复现同一问题。

这个项目演示的是“把反馈变成对象”，没有复现、验证或修复 RuoYi 缺陷，也不需要部署 RuoYi。文章截图建议依次展示：

1. 完整描述 → READY 和结构化字段。
2. 信息不足 → NEEDS_INFO 和缺失项。
3. 无关输入 → UNSUPPORTED。
4. 核心 Java 校验代码与接口 JSON。

本机真实调用结果保存在被 Git 忽略的 `.local/live-*.json`；它们是一次运行的记录，不是模型正确率评估。

## 使用边界

当前是本地教学示例，监听 `127.0.0.1`，已具备 H2 草稿持久化、人工确认与本地工单创建，但没有登录认证和外部工单系统集成。部署共享服务前应加入身份认证、用户额度、请求大小限制、审计和费用控制，并确认数据可以发送给所用模型供应商。

密钥只放入环境变量或 Git 忽略的 `.env.local`，不要写入 Java、HTTP 示例、截图或日志。本机配置文件权限设为 600。默认日志只记录请求编号、状态、耗时或安全错误码，不记录原始描述和上游报错。
