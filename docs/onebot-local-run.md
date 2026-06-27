# 真实联调启动方式

本说明用于 SnowLuma / OneBot v11 WebSocket 真实联调。不要把真实 SnowLuma token 或 Dify API Key 写入 Git。

## SnowLuma WebSocket 服务端配置

```text
host = 0.0.0.0
port = 3001
path = /
消息格式 = 数组
上报自身消息 = 关闭
启用 = 开启
```

## 准备本地 env 文件

在 `bot` 目录执行：

```powershell
copy scripts\onebot-local.env.example scripts\onebot-local.env
```

然后编辑 `scripts\onebot-local.env`，填入真实 SnowLuma token 和三个 Dify 工作流 API Key。`scripts/onebot-local.env` 已加入 `.gitignore`，不要提交。

模板中的关键项：

```text
QQBOT_ONEBOT_WS_ENABLED=true
QQBOT_ONEBOT_WS_URL=ws://127.0.0.1:3001/
QQBOT_ONEBOT_WS_TOKEN=your-snowluma-token
QQBOT_ONEBOT_SELF_ID=1771183256
QQBOT_ONEBOT_ALLOWED_GROUP_IDS=736566774

DIFY_ENABLED=true
DIFY_BASE_URL=https://api.dify.ai/v1
DIFY_MEME_SCENE_API_KEY=app-your-meme-scene-key
DIFY_PASSIVE_CHAT_API_KEY=app-your-passive-chat-key
DIFY_ACTIVE_CHAT_API_KEY=app-your-active-chat-key
DIFY_MEME_SCENE_WORKFLOW=meme-scene-recognizer
DIFY_PASSIVE_CHAT_WORKFLOW=passive-chat-reply
DIFY_ACTIVE_CHAT_WORKFLOW=active-chat-reply

QQBOT_MEME_BASE_DIR=./memes
```

## 放置表情包图片

真实图片放在本地 `memes/` 下，不提交到 Git。例如：

```text
memes/laugh/laugh_001.png
```

数据库 `meme_material.file_path` 写相对路径：

```text
laugh/laugh_001.png
```

不要写死绝对路径：

```text
C:\qqbot\memes\laugh_001.png
```

运行时后端会按 `qqbot.meme.base-dir` 解析成绝对路径，并转换成 OneBot 可识别的 `file:///C:/...` URI。图片不存在时会打印 warn 日志，但不会让主路由崩溃。

## 启动

```powershell
.\scripts\run-onebot-local.ps1
```

脚本默认使用 `local` profile。当前项目没有单独的 `application-local.yaml`，因此会加载 `application.yaml` 加本地 env 环境变量；因为不是 `dev` profile，且 `QQBOT_ONEBOT_WS_ENABLED=true` 时，会使用真实 `OneBotWsMessageSender`。

脚本启动前只会打印 profile、OneBot URL、self id、白名单群、meme base dir、Dify enabled 和三个 workflow 名称，不会打印 SnowLuma token 或 Dify API Key。
