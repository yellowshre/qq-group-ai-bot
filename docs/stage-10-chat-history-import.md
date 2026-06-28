# Stage 10 Chat History Import

## 本阶段目标

第十阶段第一版只实现离线群聊历史处理模块：

```text
JSON 导入 -> 原始消息落库 -> 清洗真实文本 -> 提取 @ / 回复关系 -> 15 分钟会话切分 -> 成员活跃统计
```

本阶段不接入 Dify、不接入向量库，也不让线上机器人回复使用历史消息。

## JSON 放置路径

导出的 QQ 群 JSON 请放到项目根目录下：

```text
data/chat-export/
```

示例：

```text
data/chat-export/group_sample_251288204_20260628.json
```

建议将导出的 JSON 重命名为纯英文、数字、下划线格式，例如：

```text
data/chat-export/group_251288204_sample_20260628_185926.json
```

避免使用中文、空格和特殊符号，尤其是：

```text
? * < > | " :
```

这样可以减少 Windows PowerShell 请求编码和路径解析问题。

接口只允许读取 `data/chat-export/` 下的文件，禁止读取任意系统路径。

## 为什么 data 不提交

`/data/` 已加入 `.gitignore`。聊天记录属于高敏感数据，不应提交到 Git，也不应进入日志。

请勿提交：

```text
聊天 JSON
真实 QQ token
Dify key
本地真实 env 文件
```

## 导入接口

仅在 `dev` / `local` profile 下启用：

```http
POST /dev/chat-history/import
```

请求体：

```json
{
  "groupId": "251288204",
  "filePath": "data/chat-export/group_sample_251288204_20260628.json"
}
```

返回体示例：

```json
{
  "batchId": 1,
  "rawMessages": 416,
  "cleanMessages": 261,
  "mentions": 29,
  "replies": 28,
  "sessions": 10,
  "members": 15,
  "status": "SUCCESS",
  "duplicateImport": false
}
```

导入前会计算 SHA-256。相同 `groupId + sourceHash` 已成功导入时，不会重复导入，会返回已有批次统计。

## 清洗规则

第一版只保留真实文本消息。

保留条件：

```text
system = false
recalled = false
content.text 非空
message_type 只保留 type_1 / type_3
elements 不包含 image/audio/file/video/forward/market_face
resources 为空
```

回复消息会额外处理：

```text
clean_text 去掉回复头
clean_text 去掉开头 @xxx
reply 关系写入 chat_message_reply
mention 关系写入 chat_message_mention
```

## 会话切分规则

同一 `groupId`、同一 `batchId` 下，按 `message_time, seq` 排序。

相邻两条 clean message 间隔超过 15 分钟，则开启新 session。

写入：

```text
chat_session
chat_session_message
```

## 验收 SQL

```sql
select count(*) from chat_import_batch;
select count(*) from chat_raw_message;
select count(*) from chat_clean_message;
select count(*) from chat_message_mention;
select count(*) from chat_message_reply;
select count(*) from chat_session;
select count(*) from chat_session_message;
select count(*) from chat_member_stat;
```

## 数据库迁移

迁移 SQL：

```text
src/main/resources/db/mysql/stage10_chat_history_migration.sql
```

首次联调前请在 MySQL 中执行该 SQL，创建第十阶段 8 张离线历史表。

## 本阶段不做

```text
Dify 接入
向量库接入
机器人线上回复使用历史消息
群梗自动生成
成员画像生成
图片识别
语音识别
主动插话引用历史记录
```
