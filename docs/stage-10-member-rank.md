# 第十阶段后半段：候选知识审批与成员排行

本阶段新增两类离线能力：

1. 基于 `chat_clean_message`、`chat_session`、`chat_member_stat` 生成候选群梗和候选成员画像。
2. 基于 `chat_member_stat`、`chat_member_stat_daily` 查询群成员数据排行。

这些能力不直接改变真实机器人主链路，默认也不会在群里公开排行。

## 候选知识生成

先导入聊天记录：

```http
POST /dev/chat-history/import
```

再生成候选：

```http
POST /dev/chat-history/candidates/generate
```

示例：

```json
{
  "batchId": 1,
  "groupId": "251288204"
}
```

生成后会写入：

- `chat_knowledge_candidate`
- `chat_member_candidate`
- `chat_knowledge_review_log`

候选默认是 `PENDING`，不会直接进入正式知识库，也不会被 Dify 或真实机器人使用。

## 如何审批

查询候选群梗：

```http
GET /dev/chat-history/candidates?batchId=1&groupId=251288204&status=PENDING
```

审批某条候选群梗：

```http
POST /dev/chat-history/candidates/{id}/review
```

示例：

```json
{
  "status": "APPROVED",
  "reviewer": "local-admin",
  "reviewComment": "确认是群内常用梗"
}
```

查询候选成员画像：

```http
GET /dev/chat-history/member-candidates?batchId=1&groupId=251288204&status=PENDING
```

审批某条成员画像：

```http
POST /dev/chat-history/member-candidates/{id}/review
```

可用状态：

```text
PENDING
APPROVED
REJECTED
DISABLED
```

只有后续发布到正式知识库、完成 embedding，并且知识库灰度开关打开后，机器人链路才会使用这些知识。

## 成员排行数据

导入聊天记录时会写入两张统计表：

- `chat_member_stat`：按成员聚合的当前批次总统计。
- `chat_member_stat_daily`：按成员、日期聚合的每日统计。

`chat_member_stat_daily` 用来支持按日期范围查询排行。未来前端可以直接复用同一个查询接口。

支持的排行类型：

| rankType | 中文含义 |
| --- | --- |
| `MESSAGE` | 发言数 |
| `RAW_MESSAGE` | 原始消息数 |
| `ACTIVE_DAYS` | 活跃天数 |
| `MENTION` | 提到别人次数 |
| `REPLY` | 回复别人次数 |
| `REPLIED_BY` | 被回复次数 |
| `SESSION` | 参与会话数 |

常用中文别名也可用于群内/私聊命令，例如 `发言`、`回复`、`被回复`、`会话`。

## HTTP 查询接口

GET 示例：

```http
GET /dev/chat-history/member-rank?groupId=251288204&rankType=MESSAGE&topN=5
```

按日期范围查询：

```http
GET /dev/chat-history/member-rank?groupId=251288204&rankType=REPLY&startDate=2026-06-01&endDate=2026-06-29&topN=10
```

POST 示例：

```http
POST /dev/chat-history/member-rank
```

```json
{
  "groupId": "251288204",
  "rankType": "MESSAGE",
  "startDate": "2026-06-01",
  "endDate": "2026-06-29",
  "topN": 5
}
```

接口只返回统计摘要，不返回聊天正文。

## 群内排行命令

默认关闭群内公开查询：

```env
QQBOT_MEMBER_RANK_GROUP_COMMAND_ENABLED=false
```

确认熟人群可以公开后再打开：

```env
QQBOT_MEMBER_RANK_ENABLED=true
QQBOT_MEMBER_RANK_GROUP_COMMAND_ENABLED=true
```

群内示例：

```text
#排行 发言 top5
#排行 回复 2026-06-01 2026-06-29 top10
#排行 被回复 前5
```

如果设置：

```env
QQBOT_MEMBER_RANK_ADMIN_ONLY=true
```

则只有 `QQBOT_ADMINS` 可以在群内查询。

## 私聊排行命令

私聊入口沿用私聊管理员模块。非管理员私聊不会有任何回应。

配置：

```env
QQBOT_MEMBER_RANK_ENABLED=true
QQBOT_MEMBER_RANK_PRIVATE_COMMAND_ENABLED=true
QQBOT_PRIVATE_ADMIN_COMMAND_ENABLED=true
QQBOT_PRIVATE_ADMIN_LIMIT_TO_ALLOWED_GROUPS=true
```

私聊示例：

```text
#群 251288204 排行 发言 top5
#群 251288204 排行 回复 2026-06-01 2026-06-29 top10
#群 251288204 排行 被回复 前5
```

私聊仍会校验：

- 操作者在 `QQBOT_ADMINS` 中。
- 目标群在 `QQBOT_ONEBOT_ALLOWED_GROUP_IDS` 中，除非关闭 `QQBOT_PRIVATE_ADMIN_LIMIT_TO_ALLOWED_GROUPS`。

## 环境变量

模板文件见：

```text
scripts/onebot-local.env.example
```

新增配置：

```env
QQBOT_MEMBER_RANK_ENABLED=false
QQBOT_MEMBER_RANK_GROUP_COMMAND_ENABLED=false
QQBOT_MEMBER_RANK_PRIVATE_COMMAND_ENABLED=true
QQBOT_MEMBER_RANK_ADMIN_ONLY=false
QQBOT_MEMBER_RANK_DEFAULT_TOP_N=5
QQBOT_MEMBER_RANK_MAX_TOP_N=10
QQBOT_MEMBER_RANK_COMMAND_PREFIX=#排行
```

## 真实群建议

建议第一阶段只开私聊查询：

```env
QQBOT_MEMBER_RANK_ENABLED=true
QQBOT_MEMBER_RANK_GROUP_COMMAND_ENABLED=false
QQBOT_MEMBER_RANK_PRIVATE_COMMAND_ENABLED=true
```

等确认数据无误、群友接受公开排行后，再打开群内命令。

排行模块只读统计表，不读取或输出完整聊天正文。
