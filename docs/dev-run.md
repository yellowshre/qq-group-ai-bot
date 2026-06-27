G# 本地开发联调

当前 dev 环境只连接本地 MySQL 和 Redis，不连接真实 NapCat，不启用正式 `.bot.json`，不调用真实 Dify。

## 1. 启动 MySQL 和 Redis

在 `bot` 目录执行：

```powershell
docker compose -f docker-compose-dev.yaml up -d
```

只会启动两个容器：

- `qqbot-mysql-dev`：MySQL 8，端口 `3306`
- `qqbot-redis-dev`：Redis 7，端口 `6379`

MySQL 首次初始化时会执行：

```text
src/main/resources/db/mysql/schema.sql
```

如果你改了 `schema.sql` 里的初始化数据，并希望重新跑初始化，需要先删除旧 volume：

```powershell
docker compose -f docker-compose-dev.yaml down -v
docker compose -f docker-compose-dev.yaml up -d
```

## 2. 使用 dev profile 启动后端

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

dev profile 会使用：

- MySQL：`jdbc:mysql://127.0.0.1:3306/qqbot`
- Redis：`127.0.0.1:6379`
- Dify：`enabled=false`
- 表情包缓存预热：`enabled=true`
- Simbot bot 自动启动：`false`
- QQ 发送器：`MockMessageSender`

启动日志里应看到类似：

```text
QQ message sender active: MockMessageSender
Meme cache preheat completed. success=true, keywords=..., scenes=...
The number of registered bots is 0
```

## 3. 开发健康检查

```http
GET http://localhost:8081/dev/health/full
```

期望要点：

```json
{
  "activeProfiles": ["dev"],
  "mysql": { "reachable": true },
  "redis": { "reachable": true },
  "difyEnabled": false,
  "memeCachePreheatEnabled": true,
  "messageSenderType": "MockMessageSender",
  "sceneDictCount": 3,
  "enabledMemeMaterialCount": 4
}
```

如果 MySQL 或 Redis 不可用，`reachable` 会是 `false`，`detail` 会给出底层错误。

## 4. 模拟群消息

使用 Apifox 或 curl 调用：

```http
POST http://localhost:8081/dev/simulate/group-message
Content-Type: application/json
```

请求体示例：

```json
{
  "groupId": "10001",
  "userId": "20001",
  "messageId": "dev-msg-001",
  "rawMessage": "哈哈",
  "atBot": false,
  "botNicknameMatched": false
}
```

注意：消息去重会使用 `messageId`，重复调用时请换一个新的 `messageId`。

如果命中示例关键词，返回里应看到：

```json
{
  "routeType": "MEME",
  "shouldSend": true,
  "memeId": 1
}
```

实际 QQ 不会发送消息，dev profile 下只会通过 `MockMessageSender` 打日志。

## 5. 验收用例

### 5.1 命中 laugh 关键词

```json
{
  "groupId": "10001",
  "userId": "20001",
  "messageId": "dev-laugh-001",
  "rawMessage": "哈哈",
  "atBot": false,
  "botNicknameMatched": false
}
```

期望：

```json
{
  "responseType": "MEME",
  "dedupPassed": true,
  "adminCommandHit": false,
  "memeHit": true,
  "shouldSend": true,
  "memeId": 1
}
```

`memeId` 可能因为权重随机不是固定值，但 `memeHit=true`、`responseType=MEME` 应成立。

### 5.2 相同 messageId 验证去重

再次发送同一个 `messageId`：

```json
{
  "groupId": "10001",
  "userId": "20001",
  "messageId": "dev-laugh-001",
  "rawMessage": "哈哈",
  "atBot": false,
  "botNicknameMatched": false
}
```

期望：

```json
{
  "responseType": "SILENT",
  "dedupPassed": false,
  "memeHit": false,
  "shouldSend": false
}
```

### 5.3 未命中关键词验证静默

换一个新的 `messageId`：

```json
{
  "groupId": "10001",
  "userId": "20001",
  "messageId": "dev-miss-001",
  "rawMessage": "今天天气不错",
  "atBot": false,
  "botNicknameMatched": false
}
```

期望：

```json
{
  "responseType": "SILENT",
  "dedupPassed": true,
  "memeHit": false,
  "shouldSend": false,
  "reason": "meme not matched"
}
```

## 6. 查看 Redis 表情包缓存

表情包缓存使用 Redis database `2`：

```powershell
docker exec -it qqbot-redis-dev redis-cli -n 2 KEYS "meme:*"
docker exec -it qqbot-redis-dev redis-cli -n 2 GET "meme:kw:哈哈"
docker exec -it qqbot-redis-dev redis-cli -n 2 GET "meme:scene:laugh"
```

缓存值是素材 ID 的 JSON 数组，例如：

```json
[1,2]
```

## 7. 查看 MySQL 触发日志

```powershell
docker exec -it qqbot-mysql-dev mysql -uqqbot -pqqbot_dev_pwd qqbot -e "select id, group_id, user_id, message_id, response_type, meme_id, success, duration_ms, error_msg, created_at from trigger_log order by id desc limit 10;"
```

也可以查看素材数据：

```powershell
docker exec -it qqbot-mysql-dev mysql -uqqbot -pqqbot_dev_pwd qqbot -e "select meme_id, keywords, scene_code, weight, enabled, file_path from meme_material;"
```

## 8. Dify 工作流 A 调试说明

默认 dev profile 中：

```yaml
qqbot:
  dify:
    enabled: false
```

因此本地普通开发、`/dev/simulate/group-message` 和单元测试都不会访问真实 Dify。

如需调试表情包语义场景识别 A 通路，可以参考 `src/main/resources/application-dify-example.yaml`，通过环境变量或单独 profile 配置：

```yaml
qqbot:
  dify:
    enabled: true
    base-url: http://127.0.0.1:5001
    workflow:
      meme-scene: ${DIFY_MEME_SCENE_WORKFLOW:meme-scene-recognizer}
      passive-chat: ${DIFY_PASSIVE_CHAT_WORKFLOW:passive-chat-reply}
      active-chat: ${DIFY_ACTIVE_CHAT_WORKFLOW:active-chat-reply}
    meme-scene-api-key: ${DIFY_MEME_SCENE_API_KEY}
    passive-chat-api-key: ${DIFY_PASSIVE_CHAT_API_KEY}
    active-chat-api-key: ${DIFY_ACTIVE_CHAT_API_KEY}
    timeout-ms: 30000
```

不要把真实 API Key 写进仓库文件。A/B/C 三个 Dify 工作流分别使用独立 Key：`DIFY_MEME_SCENE_API_KEY`、`DIFY_PASSIVE_CHAT_API_KEY`、`DIFY_ACTIVE_CHAT_API_KEY`。

Dify 工作流 A 输入：

```json
{
  "text": "这也太好笑了吧",
  "groupId": 10001,
  "userId": 20001
}
```

Dify 工作流 A 输出需要包含：

```json
{
  "sceneCode": "laugh",
  "confidence": 0.86
}
```

`sceneCode` 必须存在于 `scene_dict`，`confidence` 必须大于等于该场景的 `confidence_threshold`。通过后系统会从 `meme_material` 中按 `scene_code` 查询启用素材，并按 `weight` 随机选择。

调试用例：

```json
{
  "groupId": "10001",
  "userId": "20001",
  "messageId": "dev-dify-laugh-001",
  "rawMessage": "这也太好笑了吧",
  "atBot": false,
  "botNicknameMatched": false
}
```

期望命中时返回中能看到：

```json
{
  "routeType": "MEME",
  "memeHit": true,
  "workflowType": "MEME_DIFY_SCENE",
  "sceneCode": "laugh",
  "confidence": 0.86
}
```

如果 Dify 超时、失败、返回格式异常、`sceneCode` 不存在、置信度低于阈值，系统应返回 `SILENT`，不影响主流程。语义结果会缓存到 Redis database `2`，Key 为 `meme:cache:{textHash}`，TTL 约 1 小时。

## 9. Dify 工作流 B 被动对话调试说明

默认 dev profile 中 `qqbot.dify.enabled=false`，因此 `atBot=true` 或 `botNicknameMatched=true` 只会返回明确的静默原因，不会访问真实 Dify。

如需本地调试被动 AI 对话 B 通路，先通过环境变量提供 Dify 配置，不要把真实 Key 写入仓库文件：

```powershell
$env:DIFY_BASE_URL="https://api.dify.ai/v1"
$env:DIFY_PASSIVE_CHAT_API_KEY="你的工作流 B API Key"
$env:DIFY_PASSIVE_CHAT_WORKFLOW="passive-chat-reply"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev" "-Dspring-boot.run.arguments=--qqbot.dify.enabled=true"
```

Dify 工作流 B 输入变量：

```json
{
  "text": "@小黄 你觉得我这波操作怎么样",
  "groupId": 10001,
  "userId": 20001,
  "botName": "小黄",
  "persona": "你是一个说话简短、自然、略带吐槽但不恶意攻击人的 QQ 群机器人。",
  "recentMessages": "暂无上下文"
}
```

Dify 工作流 B 输出变量：

```json
{
  "replyText": "这波有点东西，但也别太自信。",
  "confidence": 0.86
}
```

### 9.1 atBot 触发被动 B

```json
{
  "groupId": "10001",
  "userId": "20001",
  "messageId": "passive-chat-test-001",
  "rawMessage": "@小黄 你觉得我这波操作怎么样",
  "atBot": true,
  "botNicknameMatched": false
}
```

期望返回中能看到：

```json
{
  "routeType": "PASSIVE_CHAT",
  "responseType": "PASSIVE_CHAT",
  "shouldSend": true,
  "passiveChatHit": true,
  "replyText": "有值",
  "chatConfidence": 0.86,
  "workflowType": "PASSIVE_DIFY_CHAT"
}
```

### 9.2 昵称命中触发被动 B

```json
{
  "groupId": "10001",
  "userId": "20001",
  "messageId": "passive-chat-test-002",
  "rawMessage": "小黄，你说我现在该不该睡觉",
  "atBot": false,
  "botNicknameMatched": true
}
```

期望 `passiveChatHit=true`，并由 dev profile 下的 `MockMessageSender` 模拟发送文字。

### 9.3 普通消息不触发被动 B

```json
{
  "groupId": "10001",
  "userId": "20001",
  "messageId": "passive-chat-test-003",
  "rawMessage": "今天晚上吃什么",
  "atBot": false,
  "botNicknameMatched": false
}
```

期望 `passiveChatHit=false`，不会调用 Dify 工作流 B，会继续走普通表情包 A 通路或静默。

### 9.4 文字回复后继续配图

被动 B 成功生成 `replyText` 后，系统会用 `replyText` 调用 `MemeMatchService`。如果命中素材，返回中会看到 `memeHit=true`、`memeId`、`sceneCode` 等字段，并且 `MockMessageSender` 会先模拟发送文字，再模拟发送图片。

### 9.5 Redis 上下文与 trigger_log 验收

查看 Redis 热层上下文：

```powershell
docker exec -it qqbot-redis-dev redis-cli -n 1 KEYS "*chat*"
docker exec -it qqbot-redis-dev redis-cli -n 1 LRANGE "qqbot:chat:ctx:10001" 0 -1
docker exec -it qqbot-redis-dev redis-cli -n 1 TTL "qqbot:chat:ctx:10001"
```

期望能看到用户消息和机器人回复，且 Key 有 TTL。

查看 MySQL 触发日志：

```powershell
docker exec -it qqbot-mysql-dev mysql -uqqbot -pqqbot_dev_pwd qqbot -e "select id, group_id, user_id, message_id, response_type, response_text, meme_id, workflow_type, duration_ms, success, error_msg, created_at from trigger_log order by id desc limit 10;"
```

期望 `workflow_type=PASSIVE_DIFY_CHAT`；如果回复配图命中，`meme_id` 有值。

## 10. 第七阶段主动插话 C 第一轮配置说明

第七阶段第一轮只准备主动插话 C 的基础能力；当前第二轮已经正式接入 `MessageRouterService`。下面配置仍然有效，用于控制主动插话身份、安全词和低频策略。

### 10.1 机器人业务身份

`qqbot.identity.display-name` 是业务显示名，不依赖真实 QQ 昵称。可以通过环境变量覆盖：

```yaml
qqbot:
  identity:
    display-name: ${QQBOT_DISPLAY_NAME:小黄}
    aliases:
      - ${QQBOT_ALIAS_PRIMARY:小黄}
      - ${QQBOT_ALIAS_SECONDARY:黄哥}
      - ${QQBOT_ALIAS_FALLBACK:机器人}
```

`aliases` 可以配置多个机器人别名，用于后续判断用户是不是在叫机器人。

### 10.2 被动聊天触发词

`qqbot.passive-chat.trigger-words` 用于配置不依赖真实 QQ 昵称的被动聊天触发词：

```yaml
qqbot:
  passive-chat:
    trigger-words:
      - 小黄
      - 黄哥
      - 机器人
```

### 10.3 主动插话安全词

主动插话安全词和被动聊天触发词分开配置：

```yaml
qqbot:
  safety:
    admin-only: true
    active-chat-off-words:
      - "#autochatoff"
      - "#停用主动插话"
      - "#别插话"
      - "小黄闭嘴"
    active-chat-on-words:
      - "#autochaton"
      - "#开启主动插话"
      - "#可以说话了"
      - "小黄说话"
```

当前 `admin-only` 默认是 `true`。这些安全词会在管理员权限通过后更新群配置里的主动插话开关。

### 10.4 主动插话参数

```yaml
qqbot:
  active-chat:
    enabled: true
    cooldown-seconds: 180
    max-per-hour: 20
    random-probability: 1.0
    min-confidence: 0.6
    min-message-length: 3
    max-message-length: 80
    allow-after-meme-sent: false
    allow-after-bot-message: false
```

开发验收时 `random-probability` 可以设为 `1.0`。后续真实体验可调低，例如 `0.15` 到 `0.3`。

### 10.5 Dify 工作流 C

Dify 工作流 C 名称为 `active-chat-reply`。第七阶段第一轮只新增后端调用能力；第二轮已经接入 `MessageRouterService` 主路由。

```yaml
qqbot:
  dify:
    workflow:
      active-chat: ${DIFY_ACTIVE_CHAT_WORKFLOW:active-chat-reply}
    active-chat-api-key: ${DIFY_ACTIVE_CHAT_API_KEY:}
```

`DIFY_ACTIVE_CHAT_API_KEY` 必须使用环境变量，不要写入真实 Key。Dify C 输入里的 `groupId`、`userId` 会按字符串传给 Dify。

## 11. 第七阶段主动插话 C 第二轮验收

主动插话 C 现在已经接入 `MessageRouterService`，触发顺序固定为：

```text
去重 -> 群配置 -> 群总开关 -> 管理员指令/安全词 -> 被动聊天 B -> 普通表情包 A -> 主动插话 C -> 静默
```

也就是说，`atBot=true` 或 `botNicknameMatched=true` 时仍然优先进入被动聊天 B；普通消息如果命中表情包 A，会直接发送表情包，不会继续触发 C。只有 B 未命中、A 未发送，并且主动插话策略通过时，才会调用 Dify C。

主动插话由这些配置控制：

- `qqbot.active-chat.cooldown-seconds`：每个群主动插话冷却时间。
- `qqbot.active-chat.max-per-hour`：每个群每小时最多主动插话次数。
- `qqbot.active-chat.random-probability`：随机触发概率；开发验收可以设为 `1.0`，真实体验建议调低到 `0.15` 到 `0.3`。
- `qqbot.active-chat.min-confidence`：Dify C 回复最低置信度。
- `qqbot.safety.active-chat-off-words`：关闭主动插话，例如 `#autochatoff`。
- `qqbot.safety.active-chat-on-words`：开启主动插话，例如 `#autochaton`。

dev profile 下仍然使用 `MockMessageSender`，不会连接真实 QQ 或 NapCat。没有配置 `DIFY_ACTIVE_CHAT_API_KEY` 时，Dify C 会静默失败，不影响 A/B 主流程。

如需本地调试 Dify C，请只通过环境变量提供 Key：

```powershell
$env:DIFY_BASE_URL="https://api.dify.ai/v1"
$env:DIFY_ACTIVE_CHAT_API_KEY="你的工作流 C API Key"
$env:DIFY_ACTIVE_CHAT_WORKFLOW="active-chat-reply"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev" "-Dspring-boot.run.arguments=--qqbot.dify.enabled=true --qqbot.active-chat.random-probability=1.0"
```

模拟一条可能触发 C 的普通群消息：

```json
{
  "groupId": "10001",
  "userId": "20001",
  "messageId": "active-chat-test-001",
  "rawMessage": "今天这局打得有点抽象",
  "atBot": false,
  "botNicknameMatched": false
}
```

命中时 `/dev/simulate/group-message` 应能看到：

```json
{
  "routeType": "ACTIVE_CHAT",
  "responseType": "ACTIVE_CHAT",
  "shouldSend": true,
  "activeChatHit": true,
  "activePolicyPassed": true,
  "activeShouldReply": true,
  "workflowType": "ACTIVE_DIFY_CHAT",
  "replyText": "有值",
  "activeConfidence": 0.8,
  "silentReason": null
}
```

主动插话成功发送后会写入 Redis 热层上下文，并调用 `ActiveChatPolicyService.markActiveChatSent(...)` 写入冷却和小时计数：

```powershell
docker exec -it qqbot-redis-dev redis-cli -n 1 LRANGE "qqbot:chat:ctx:10001" 0 -1
docker exec -it qqbot-redis-dev redis-cli -n 3 KEYS "qqbot:active:*"
```

成功发送后也会写入 `trigger_log`，其中 `response_type=ACTIVE_CHAT`、`workflow_type=ACTIVE_DIFY_CHAT`：

```powershell
docker exec -it qqbot-mysql-dev mysql -uqqbot -pqqbot_dev_pwd qqbot -e "select id, group_id, user_id, message_id, response_type, response_text, workflow_type, success, error_msg, created_at from trigger_log order by id desc limit 10;"
```

如果策略拒绝、Dify C 失败、`shouldReply=false`、回复为空或置信度低于阈值，系统会返回 `SILENT`，并且不会写入 trigger_log、不会写主动插话冷却。
