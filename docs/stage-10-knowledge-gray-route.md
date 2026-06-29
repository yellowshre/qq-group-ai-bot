# 第十阶段第五版：知识库上下文灰度接入真实链路

本阶段把已经验证过的 `KnowledgeContextService` 灰度接入 A/B/C 三条 Dify 链路。

默认情况下所有群级知识库开关都是关闭的，现有机器人行为不变。只有在指定群的 `group_config` 中显式打开开关后，才会把经过审核的短 `knowledgeContext` 传给 Dify。

## 数据库开关

`group_config` 新增字段：

| 字段 | 默认值 | 含义 |
| --- | --- | --- |
| `enable_knowledge_context` | `0` | 知识库上下文总开关 |
| `enable_meme_knowledge` | `0` | A 表情包场景识别是否带知识 |
| `enable_passive_chat_knowledge` | `0` | B 被动聊天是否带知识 |
| `enable_active_chat_knowledge` | `0` | C 主动插话是否带知识 |

已有数据库可执行：

```sql
source src/main/resources/db/mysql/stage10_knowledge_gray_route_migration.sql;
```

## 接入策略

| 链路 | Dify 输入 | 知识类型 |
| --- | --- | --- |
| A 表情包场景识别 | `knowledgeContext` | `PHRASE`, `MEME`, `MEME_SCENE` |
| B 被动聊天 | `knowledgeContext` | `PHRASE`, `TOPIC`, 少量 `MEMBER_PROFILE` |
| C 主动插话 | `knowledgeContext` | 低风险 `PHRASE`, `TOPIC` |

推荐真实群第一轮只打开：

```text
enable_knowledge_context = true
enable_meme_knowledge = true
enable_passive_chat_knowledge = false
enable_active_chat_knowledge = false
```

这样先让 A 更懂群梗，B/C 继续保持原行为。

## 管理员命令

已支持：

```text
#开启知识库
#关闭知识库
#开启表情包知识
#关闭表情包知识
#开启聊天知识
#关闭聊天知识
#开启主动知识
#关闭主动知识
#状态
```

`#状态` 会显示知识库总开关、表情包知识开关、聊天知识开关、主动知识开关。

## 诊断接口

```http
GET /dev/health/full?groupId=251288204
```

会返回：

```text
knowledgeEmbeddingEnabled
knowledgeContextEnabled
memeKnowledgeEnabled
passiveChatKnowledgeEnabled
activeChatKnowledgeEnabled
knowledgeContextConfig
groupConfig
```

路由预览接口：

```http
POST /dev/chat-history/knowledge/context/route-preview
```

请求示例：

```json
{
  "groupId": "251288204",
  "messageText": "这个操作也太经典了",
  "senderUid": "10001",
  "topK": 5
}
```

返回会分别展示 `MEME`、`PASSIVE_CHAT`、`ACTIVE_CHAT` 在当前群配置下是否会带知识。

## 安全边界

主链路只使用正式知识库中的可用知识：

- `ACTIVE`
- `enabled = true`
- embedding 已成功生成并可检索
- 人工审核后发布的知识

不会传给 Dify：

- 原始聊天记录
- `clean_message` 全文
- `PENDING` 候选知识
- `REJECTED` 候选知识
- `DISABLED` 正式知识

日志只记录：

```text
groupId
routeType
knowledgeUsed
itemCount
maxScore
durationMs
```

不要在日志中输出完整 `knowledgeContext`、完整聊天正文或成员画像全文。

## 降级行为

知识召回失败时，主链路继续执行：

```text
knowledgeContext = ""
knowledgeUsed = false
```

这不会阻止 A/B/C 调用 Dify，也不会导致机器人异常退出。
