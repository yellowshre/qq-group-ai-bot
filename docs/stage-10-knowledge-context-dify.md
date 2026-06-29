# 第十阶段第四版：知识上下文与 Dify 输入验证层

本阶段目标是在正式知识库和本地向量检索已经验证后，增加一层受控的知识上下文服务。

本阶段仍然不接真实 OneBot 主链路，不修改 `MessageRouterService`，不让线上机器人自动使用知识库。

流程是：

```text
群消息或模拟输入
-> Spring Boot 检索正式知识库
-> 按路由类型过滤知识
-> 组装短小 knowledgeContext
-> 返回将要传给 Dify A/B/C 的 inputs
```

## 配置

```yaml
qqbot:
  knowledge:
    context:
      max-items: 5
      max-length: 800
      min-score: 0.3
      member-profile-limit: 1
      max-search-candidates: 20
      max-item-content-length: 160
```

环境变量：

```powershell
$env:QQBOT_KNOWLEDGE_CONTEXT_MAX_ITEMS="5"
$env:QQBOT_KNOWLEDGE_CONTEXT_MAX_LENGTH="800"
$env:QQBOT_KNOWLEDGE_CONTEXT_MIN_SCORE="0.3"
$env:QQBOT_KNOWLEDGE_CONTEXT_MEMBER_PROFILE_LIMIT="1"
```

向量检索仍然依赖第三版的 Ollama embedding 配置：

```powershell
$env:QQBOT_KNOWLEDGE_EMBEDDING_ENABLED="true"
$env:OLLAMA_BASE_URL="http://127.0.0.1:11434"
$env:OLLAMA_EMBEDDING_MODEL="bge-m3"
```

## 路由类型

`routeType` 支持：

- `MEME`
- `PASSIVE_CHAT`
- `ACTIVE_CHAT`

不同路由使用的知识范围：

| routeType | 可使用知识 |
| --- | --- |
| MEME | `PHRASE`, `MEME`, `MEME_SCENE` |
| PASSIVE_CHAT | `PHRASE`, `TOPIC`, 少量 `MEMBER_PROFILE` |
| ACTIVE_CHAT | 低风险 `PHRASE`, `TOPIC` |

`ACTIVE_CHAT` 不会带成员画像，避免主动 cue 敏感成员。

## 预览知识上下文

```http
POST /dev/chat-history/knowledge/context/preview
```

请求：

```json
{
  "groupId": "251288204",
  "messageText": "这个操作也太经典了",
  "senderUid": "10001",
  "routeType": "MEME",
  "topK": 5
}
```

响应示例：

```json
{
  "routeType": "MEME",
  "query": "这个操作也太经典了",
  "knowledgeUsed": true,
  "knowledgeContext": "Reviewed group knowledge only:\n1. [PHRASE] classic: classic content Hint: Use to understand reviewed group phrase meaning. Score: 0.82",
  "items": [
    {
      "targetType": "GROUP_KNOWLEDGE",
      "targetId": 1,
      "type": "PHRASE",
      "title": "classic",
      "content": "classic content",
      "score": 0.82,
      "usageHint": "Use to understand reviewed group phrase meaning."
    }
  ],
  "silentReason": null
}
```

## Dify inputs 模拟

```http
POST /dev/chat-history/dify-context/simulate
```

请求：

```json
{
  "groupId": "251288204",
  "messageText": "这个操作也太经典了",
  "senderUid": "10001",
  "routeType": "PASSIVE_CHAT",
  "topK": 5,
  "botName": "小黄",
  "persona": "你是一个简短自然的群机器人。",
  "recentMessages": "用户A：刚才那个操作太经典了"
}
```

响应会返回将要传给 Dify 的 `inputs`，但不会真的调用 Dify：

```json
{
  "routeType": "PASSIVE_CHAT",
  "workflow": "passive-chat-reply",
  "query": "这个操作也太经典了",
  "knowledgeUsed": true,
  "knowledgeContext": "...",
  "items": [],
  "inputs": {
    "text": "这个操作也太经典了",
    "groupId": "251288204",
    "userId": "10001",
    "knowledgeContext": "...",
    "botName": "小黄",
    "persona": "你是一个简短自然的群机器人。",
    "recentMessages": "用户A：刚才那个操作太经典了"
  }
}
```

## 安全限制

本阶段只使用正式库：

- 不读取 `chat_raw_message`
- 不读取 `chat_clean_message`
- 不读取未审核候选知识
- 不使用 `DISABLED` 或非 `ACTIVE` 的正式知识
- 不传低于 `min-score` 的召回结果
- `knowledgeContext` 默认最多 800 字
- 每次最多 3 到 5 条，默认 5 条
- 成员画像只允许在 `PASSIVE_CHAT` 使用，默认最多 1 条
- 日志不输出完整聊天内容

## 验收建议

1. 确认第三版正式知识和 embedding 已生成。
2. 调用 `/dev/chat-history/knowledge/context/preview`，检查不同 routeType 的过滤结果。
3. 调用 `/dev/chat-history/dify-context/simulate`，检查 `inputs.knowledgeContext` 是否短小、可控。
4. 确认 Dify 后台没有新增运行记录，因为本阶段不真实调用 Dify。
5. 确认真实 OneBot 群消息主链路没有变化。
