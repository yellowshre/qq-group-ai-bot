# Stage 10 Knowledge Candidates

## 本阶段目标

第十阶段第二版只实现离线候选知识生成与人工审核机制：

```text
chat_clean_message / chat_session / chat_member_stat
-> 候选群梗、候选短语、候选接话模式、候选话题、候选核心成员
-> PENDING 状态
-> 人工审核为 APPROVED / REJECTED / DISABLED
```

本阶段仍然不接 Dify、不接 Ollama、不接向量库，也不修改 `MessageRouterService`。

## 候选知识类型

`chat_knowledge_candidate.candidate_type`：

```text
PHRASE
MEME
REPLY_PATTERN
TOPIC
MEME_SCENE
```

`chat_member_candidate` 单独保存核心成员候选。

## 候选状态

```text
PENDING
APPROVED
REJECTED
DISABLED
```

候选知识必须先进入 `PENDING`。审核通过后也只表示“可进入后续正式知识库构建流程”，不会在本阶段直接进入向量库或被线上机器人使用。

## 自动候选生成规则

### 候选短语 / 群梗

基于 `chat_clean_message.clean_text`：

```text
clean_text 长度 2 到 30
排除纯数字、纯标点、纯符号
相同 clean_text 出现次数 >= 3
出现成员数 >= 2
```

生成：

```text
candidate_type = PHRASE
status = PENDING
hit_count = 出现次数
member_count = 出现成员数
```

### 候选接话模式

基于带回复关系的 `chat_clean_message`：

```text
is_reply = true
clean_text 长度 2 到 40
同 clean_text 出现次数 >= 2
```

生成：

```text
candidate_type = REPLY_PATTERN
evidence_text = 被回复内容摘要 + 当前回复内容
```

### 候选话题

基于 `chat_session`：

```text
session.message_count >= 10
session.member_count >= 3
```

`title` 使用会话开始时间，`content` 保存该 session 前若干条 clean text 拼接摘要，最多 500 字。

### 候选核心成员

基于 `chat_member_stat`：

```text
score =
message_count * 1
+ active_days * 5
+ mention_count * 2
+ reply_count * 2
+ replied_by_count * 2
+ session_count * 3
```

候选规则：

```text
message_count >= 10
或 score 排名前 10
```

## 人工新增候选梗

接口：

```http
POST /dev/chat-history/candidates/manual
```

示例：

```json
{
  "batchId": 1,
  "groupId": "251288204",
  "candidateType": "PHRASE",
  "title": "典",
  "content": "典",
  "evidenceText": "群友常用，表示这个操作很经典或很抽象",
  "reviewer": "local-admin",
  "reviewComment": "人工补充候选梗"
}
```

人工新增内容仍然只是候选知识：

```text
status = PENDING
hit_count = 1
member_count = 1
confidence = 1.0
```

它不能绕过审核直接进入正式知识库，也不会进入向量库。

同一 `batchId + groupId + candidateType + content` 已存在 `PENDING` 或 `APPROVED` 时，不重复插入，会返回已有记录并标记 `duplicate=true`。

前端入口：

```text
/admin/knowledge -> 手工补录 / 审批日志
```

页面会复用顶部的 `groupId`、`batchId`、操作人和备注。手工补录成功后仍需要在候选群梗表里审核通过，再发布到正式知识库。

## 接口示例

生成候选：

```http
POST /dev/chat-history/candidates/generate
```

```json
{
  "batchId": 1,
  "groupId": "251288204"
}
```

查询候选知识：

```http
GET /dev/chat-history/candidates?batchId=1&groupId=251288204&status=PENDING
```

审核候选知识：

```http
POST /dev/chat-history/candidates/{id}/review
```

```json
{
  "status": "APPROVED",
  "reviewer": "local-admin",
  "reviewComment": "确认是群内常用梗"
}
```

查询核心成员候选：

```http
GET /dev/chat-history/member-candidates?batchId=1&groupId=251288204&status=PENDING
```

审核核心成员候选：

```http
POST /dev/chat-history/member-candidates/{id}/review
```

查询审核流水：

```http
GET /dev/chat-history/review-logs?targetType=KNOWLEDGE_CANDIDATE&targetId=1
```

## 为什么本阶段不接向量库

本阶段目标是先把候选层和人工审核流程做稳。候选内容可能包含误判、噪声或只在局部上下文成立的表达，如果直接进入向量库，会污染后续检索结果。

## 为什么候选知识不能直接给机器人使用

候选知识处于统计结果或人工草稿状态，不代表已经被确认适合线上回复。线上机器人只能在后续阶段使用正式 knowledge 表和经过审核、构建、检索验证的知识。

## 验收流程

```text
1. 已有 batchId=1 的导入数据
2. POST /dev/chat-history/candidates/generate
3. GET /dev/chat-history/candidates?batchId=1&groupId=251288204&status=PENDING
4. 审核一条为 APPROVED
5. 审核一条为 REJECTED
6. GET /dev/chat-history/review-logs
7. 再次 generate，确认不会覆盖已审核结果，也不会无限重复插入
```
