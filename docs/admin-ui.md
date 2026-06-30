# Admin UI 控制台

`admin-ui/` 是 QQbot 的本地运维前端，使用：

- Vue 3
- Vite
- TypeScript
- Element Plus

第一版目标是先把干净、可中断的控制台骨架立起来，不影响真实 QQ、Dify、OneBot 主链路。

## 本阶段已有页面

```text
/admin/              总览
/admin/groups        群配置编辑器
/admin/memes         表情包素材管理
/admin/knowledge     知识候选审批工作台
/admin/member-rank   成员排行查询
/admin/simulate      群消息模拟与路由调试
/admin/logs          运行日志诊断
/admin/settings      运行配置诊断
```

当前已接入的后端接口：

```http
GET /dev/health/full
GET /dev/admin/overview
GET /dev/admin/groups
GET /dev/admin/groups/{groupId}
PUT /dev/admin/groups/{groupId}
GET /dev/admin/memes/scenes
PUT /dev/admin/memes/scenes/{sceneCode}
GET /dev/admin/memes/materials
GET /dev/admin/memes/materials/{memeId}
POST /dev/admin/memes/materials
PUT /dev/admin/memes/materials/{memeId}
POST /dev/admin/memes/cache/preheat
GET /dev/admin/logs/triggers
GET /dev/admin/logs/admin-ops
POST /dev/simulate/group-message
GET /dev/chat-history/member-rank
POST /dev/chat-history/member-rank
POST /dev/chat-history/candidates/generate
GET /dev/chat-history/candidates
POST /dev/chat-history/candidates/{id}/review
GET /dev/chat-history/member-candidates
POST /dev/chat-history/member-candidates/{id}/review
POST /dev/chat-history/knowledge/publish
POST /dev/chat-history/member-profiles/publish
GET /dev/chat-history/knowledge
GET /dev/chat-history/member-profiles
```

页面按模块独立接入 API，避免前端改动影响真实 QQ、Dify、OneBot 主链路。

## API 响应兼容

当前后端先统一错误响应，格式为：

```json
{
  "success": false,
  "code": "BAD_REQUEST",
  "message": "错误摘要",
  "data": null,
  "timestamp": "2026-06-30T00:00:00Z"
}
```

成功响应暂时保留现有裸 DTO / list / map，前端请求客户端已兼容未来的 `{ "success": true, "data": ... }` 包装格式。等管理台页面稳定后，可以再逐步把 `/dev/*` 和 `/dev/admin/*` 的成功响应统一成同一 envelope。

## Admin API Token

控制台默认不启用额外 token，保持本地开发方便：

```env
QQBOT_ADMIN_UI_API_TOKEN_ENABLED=false
QQBOT_ADMIN_UI_API_TOKEN=
```

如果需要给 `/dev/*` 管理接口加一层本地保护，可以在 `scripts/onebot-local.env` 中开启：

```env
QQBOT_ADMIN_UI_API_TOKEN_ENABLED=true
QQBOT_ADMIN_UI_API_TOKEN=your-local-admin-ui-token
```

启用后，前端右上角输入同一个 token 并保存。前端会把它保存在浏览器 `localStorage`，请求时通过：

```text
X-QQBOT-ADMIN-TOKEN
```

发送给后端。后端启动日志只打印 `enabled` 和 `tokenConfigured`，不会打印真实 token。没有配置 token 或开关为 `false` 时，现有 dev/local 接口行为不变。

`/admin/settings` 会显示：

- token 开关是否开启
- token 是否已配置
- `/dev/*` 是否处于受保护状态

这些字段同样只返回布尔值，不返回真实 token。

## 群配置编辑器

`/admin/groups` 会读取 `allowedGroupIds` 和数据库里已经存在的 `group_config`，也支持手动输入群号载入默认配置。

当前可维护：

- 机器人总开关、聊天总开关
- 表情包 A、被动聊天 B、主动插话 C
- 知识库总开关、A/B/C 知识灰度开关
- 主动插话冷却、每小时上限、每日上限
- 群级人设、安全词、安全词回复
- 记忆模式

这些配置通过 dev/local 下的 `/dev/admin/groups` 接口写入数据库，不修改 `.env`、Dify Key、SnowLuma token 或 OneBot 连接参数。敏感运行参数仍然只通过本地 env 文件和后端配置管理。

## 总览统计

`/admin/` 首页除了 `/dev/health/full`，还会读取：

```http
GET /dev/admin/overview
GET /dev/admin/overview?groupId=251288204
```

用于展示非敏感聚合数据：

- 聊天导入批次、raw / clean 消息数量、session 数、成员统计数量
- 候选群梗、候选成员画像、待审批数量
- 正式知识、正式成员画像、成功 embedding 数量
- 今日 `trigger_log` 和 `admin_op_log` 数量
- 最近一次 `chat_import_batch` 摘要

该接口只做 `count(*)` 和最近导入批次摘要，不返回聊天正文、候选内容、成员画像全文或任何密钥。若第十阶段迁移表尚不存在，对应字段会返回空值，不影响首页健康检查。

## 表情包素材管理

`/admin/memes` 当前接入 `scene_dict` 和 `meme_material`：

- 查看、编辑、新增场景字典。
- 查看、筛选、新增、编辑表情包素材。
- 维护关键词、sceneCode、sceneDesc、weight、enabled、filePath。
- 保存后会尝试刷新 Redis 表情包缓存，也可以手动点击“刷新缓存”。

图片路径仍然建议写相对路径，例如：

```text
laugh/laugh_001.png
awkward/awkward_001.png
comfort/comfort_001.jpg
```

素材管理不会上传真实图片，只维护数据库里的路径和匹配元数据。图片文件仍放在项目 `memes/` 目录或 `QQBOT_MEME_BASE_DIR` 指向的目录下。

## 知识库工作台

`/admin/knowledge` 当前接入候选知识和候选成员画像的本地审批闭环：

- 按 `groupId`、`batchId`、候选状态查询候选群梗和候选成员画像。
- 使用 `batchId + groupId` 触发候选生成。
- 对候选执行通过或拒绝，写入现有 review log。
- 选择已通过候选后发布到正式知识库或正式成员画像。
- 查看当前群的正式知识和正式成员画像。
- 启用或停用正式知识和正式成员画像，便于灰度撤回。
- 对正式知识和成员画像生成 embedding。
- 搜索正式知识，验证向量召回结果。
- 预览单条消息在 A/B/C 链路上的 `knowledgeContext` 和 Dify inputs。

对应接口：

```http
POST /dev/chat-history/import
POST /dev/chat-history/candidates/generate
GET /dev/chat-history/candidates
POST /dev/chat-history/candidates/{id}/review
GET /dev/chat-history/member-candidates
POST /dev/chat-history/member-candidates/{id}/review
POST /dev/chat-history/knowledge/publish
POST /dev/chat-history/member-profiles/publish
GET /dev/chat-history/knowledge
GET /dev/chat-history/member-profiles
POST /dev/chat-history/knowledge/embeddings/generate
POST /dev/chat-history/knowledge/search
POST /dev/chat-history/knowledge/context/preview
POST /dev/chat-history/knowledge/context/route-preview
POST /dev/chat-history/dify-context/simulate
```

页面只展示候选内容、证据摘要、正式知识和受控 `knowledgeContext` 预览，不做完整聊天原文浏览。

## 运行日志诊断

`/admin/logs` 当前接入：

- `trigger_log`：查看真实群消息路由结果、responseType、workflowType、memeId、耗时、成功状态和错误摘要。
- `admin_op_log`：查看群内/私聊管理员命令修改过哪些群配置。

筛选项包括：

- 群号、用户、messageId
- responseType、workflowType、success
- 操作人、operation
- 返回数量，默认 100，后端最多 500

为避免页面直接铺满长文本，日志接口会截断 `originalMsg`、`responseText`、`errorMsg` 和操作详情。需要完整原文时再从数据库按 `id` 或 `messageId` 精查。

## 消息模拟

`/admin/simulate` 接入：

```http
POST /dev/simulate/group-message
```

用于在浏览器里构造一条内部 `BotGroupMessage`，完整经过当前后端路由：

```text
去重 -> 群配置 -> 总开关 -> 管理员指令/安全词 -> 被动聊天 B -> 表情包 A -> 主动插话 C -> 静默
```

页面会展示：

- `routeType`、`responseType`、`shouldSend`
- `dedupPassed`、`adminCommandHit`
- `memeHit`、`passiveChatHit`、`activeChatHit`
- `workflowType`、`sceneCode`、`confidence`、`memeId`
- `replyText`、`chatConfidence`
- 主动插话策略字段，例如 `activePolicyRejectReason`、`cooldownSeconds`、`randomHit`
- 完整 `RouteResult JSON`

这个接口当前只在 `dev` profile 下启用。真实 `local` SnowLuma 联调主要看 `/admin/logs` 和真实群消息；需要在浏览器里模拟时，用 dev profile 启动后端。

## 开发启动

首次安装依赖：

```powershell
cd admin-ui
npm.cmd install --cache .\.npm-cache
```

启动前端：

```powershell
npm.cmd run dev --cache .\.npm-cache
```

访问：

```text
http://127.0.0.1:5173/admin/
```

Vite 会把 `/dev/*` 请求代理到：

```text
http://127.0.0.1:8081
```

所以后端 dev/local profile 需要运行在 8081。

## 构建

```powershell
cd admin-ui
npm.cmd run build --cache .\.npm-cache
```

构建产物输出到：

```text
admin-ui/dist/
```

`node_modules/`、`.npm-cache/`、`dist/` 已加入 `.gitignore`。

## Spring Boot 托管

前端构建完成后，Maven 会在后端 `process-resources` 阶段把 `admin-ui/dist` 复制到：

```text
target/classes/static/admin/
```

因此可以用 Spring Boot 直接访问：

```text
http://127.0.0.1:8081/admin/
http://127.0.0.1:8081/admin/groups
http://127.0.0.1:8081/admin/knowledge
```

推荐顺序：

```powershell
cd admin-ui
npm.cmd run build --cache .\.npm-cache
cd ..
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

`admin-ui/dist/` 不提交到 Git。如果没有先构建前端，Spring Boot 仍可启动，但 `/admin/` 静态页面不会存在。

## 设计原则

- 第一屏就是控制台，不做营销页。
- 黑白灰为主，少量蓝色强调。
- 不显示真实 SnowLuma token、Dify API Key。
- 敏感配置只展示 `configured=true/false`。
- 群配置、知识审批、成员排行都按独立模块推进，便于中途暂停再继续。

## 后续分步

建议继续按这些独立提交推进：

```text
feat(admin-api): add group config admin endpoints
feat(admin-ui): add group config editor
feat(admin-ui): add knowledge candidate review workspace
feat(admin-ui): add runtime settings status
```

当前阶段仍推荐开发时用 Vite 独立运行，真实本地验收时用 Spring Boot 托管构建产物。
