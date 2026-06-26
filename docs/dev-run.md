# 本地开发联调

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
    api-key: ${DIFY_API_KEY}
    workflow:
      meme-scene: ${DIFY_WORKFLOW_MEME_SCENE}
    timeout-ms: 30000
```

不要把真实 `api-key` 写进仓库文件。

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
$env:DIFY_API_KEY="你的工作流 B API Key"
$env:DIFY_WORKFLOW_PASSIVE_CHAT="passive-chat-reply"
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
