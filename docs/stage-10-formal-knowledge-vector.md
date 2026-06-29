# 第十阶段第三版：正式知识库与本地向量检索

本阶段目标是在聊天记录离线导入、清洗、候选梗筛选和人工审核之后，建立一层正式知识库，并用本地 Ollama embedding 做向量检索验证。

本阶段仍然不接入 Dify、不接入 OneBot、不修改 `MessageRouterService`。在线机器人暂时不会读取这批知识。

## 候选库和正式库

候选库包括：

- `chat_knowledge_candidate`
- `chat_member_candidate`

候选库用于保存自动筛出的梗、话题、回复模式和成员候选画像。候选内容需要人工审核，只有 `APPROVED` 状态才允许发布到正式库。

正式库包括：

- `chat_group_knowledge`
- `chat_member_profile`

正式库用于保存已经审核通过、可被后续检索和知识注入使用的内容。正式库支持启用、禁用和归档状态。当前接口只做发布、启用、禁用，不做物理删除。

## 为什么要审核后再发布

聊天记录里会有玩笑、误解、临时上下文和不适合作为长期知识的内容。候选库只表示“可能有价值”，不表示“可以被机器人长期使用”。

人工审核能避免把错误梗、敏感内容、过期称呼和低质量成员画像直接写入正式库。

## 数据库脚本

新增脚本：

```text
src/main/resources/db/mysql/stage10_formal_knowledge_vector_migration.sql
```

执行后会新增：

- `chat_group_knowledge`
- `chat_member_profile`
- `chat_knowledge_embedding`
- `chat_knowledge_publish_log`

## 发布接口

发布群知识：

```http
POST /dev/chat-history/knowledge/publish
```

请求示例：

```json
{
  "groupId": "251288204",
  "candidateIds": [1, 2, 3],
  "operator": "local-admin",
  "comment": "第一批人工确认"
}
```

发布成员画像：

```http
POST /dev/chat-history/member-profiles/publish
```

只会发布 `APPROVED` 候选。重复发布同一个来源候选会跳过，不会重复插入正式库。

## 查询和启停

查询正式知识：

```http
GET /dev/chat-history/knowledge?groupId=251288204&enabled=true
```

查询成员画像：

```http
GET /dev/chat-history/member-profiles?groupId=251288204&enabled=true
```

禁用或启用：

```http
POST /dev/chat-history/knowledge/{id}/disable
POST /dev/chat-history/knowledge/{id}/enable
POST /dev/chat-history/member-profiles/{id}/disable
POST /dev/chat-history/member-profiles/{id}/enable
```

## Ollama Embedding 配置

默认关闭：

```yaml
qqbot:
  knowledge:
    embedding:
      enabled: false
      provider: ollama
      base-url: http://127.0.0.1:11434
      model: bge-m3
      endpoint-path: /api/embed
      timeout-seconds: 30
```

本地验证前设置环境变量：

```powershell
$env:QQBOT_KNOWLEDGE_EMBEDDING_ENABLED="true"
$env:QQBOT_KNOWLEDGE_EMBEDDING_PROVIDER="ollama"
$env:OLLAMA_BASE_URL="http://127.0.0.1:11434"
$env:OLLAMA_EMBEDDING_MODEL="bge-m3"
$env:OLLAMA_EMBEDDING_ENDPOINT="/api/embed"
```

Ollama 模型建议：

```powershell
ollama pull bge-m3
```

当前实现优先兼容 `/api/embed`：

```json
{
  "model": "bge-m3",
  "input": "..."
}
```

响应：

```json
{
  "model": "bge-m3",
  "embeddings": [[0.1, 0.2, 0.3]]
}
```

也兼容旧式 `/api/embeddings` 的 `embedding` 字段。

## 生成向量

```http
POST /dev/chat-history/knowledge/embeddings/generate
```

请求示例：

```json
{
  "groupId": "251288204",
  "targetTypes": ["GROUP_KNOWLEDGE", "MEMBER_PROFILE"],
  "regenerate": false
}
```

`regenerate=false` 时，如果同一目标、同一模型、同一文本 hash 已经有成功 embedding，会跳过。

Ollama 不可用时，导入、候选生成和审核不受影响。embedding 生成接口会记录失败数量和失败状态。

日志只输出：

- targetType
- targetId
- model
- dimension
- status

不输出完整聊天正文。

## 向量检索验证

```http
POST /dev/chat-history/knowledge/search
```

请求示例：

```json
{
  "groupId": "251288204",
  "query": "群里说的典是什么意思",
  "topK": 5,
  "targetTypes": ["GROUP_KNOWLEDGE"]
}
```

后端会：

1. 用 Ollama 生成 query embedding。
2. 读取同群成功生成的正式知识或成员画像 embedding。
3. 在 Java 内存中计算 cosine similarity。
4. 返回 topK。

当前只是验证检索效果，不会把检索结果注入 B/C 工作流。

## 验收建议

1. 执行 `stage10_formal_knowledge_vector_migration.sql`。
2. 确认若干 `chat_knowledge_candidate` / `chat_member_candidate` 已经是 `APPROVED`。
3. 调用发布接口，确认正式库新增数据。
4. 启动 Ollama 并拉取 `bge-m3`。
5. 开启 `QQBOT_KNOWLEDGE_EMBEDDING_ENABLED=true`。
6. 调用 embedding 生成接口。
7. 调用 search 接口，用明显相关的问题验证排序。
8. 确认在线机器人主流程没有变化。
