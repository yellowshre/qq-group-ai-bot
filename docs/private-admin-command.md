# 私聊管理员控制模块

私聊管理员控制是备用运维入口，不替代未来前端后台。

推荐架构：

```text
GroupConfigAdminService
  -> PrivateAdminCommandService
  -> future Admin REST API
```

群内控制逻辑保持不变。私聊控制只处理 OneBot `message_type=private` 消息，不进入 `MessageRouterService` 的 A/B/C 主链路。

## 环境变量

```text
QQBOT_PRIVATE_ADMIN_COMMAND_ENABLED=false
QQBOT_PRIVATE_ADMIN_LIMIT_TO_ALLOWED_GROUPS=true
QQBOT_PRIVATE_ADMIN_COMMAND_PREFIX=#

QQBOT_CMD_ACTIVE_CHAT_OFF_WORDS=#autochatoff,#停用主动插话,#别插话,茉优先安静
QQBOT_CMD_ACTIVE_CHAT_ON_WORDS=#autochaton,#开启主动插话,#可以说话了,茉优可以说话了

QQBOT_ADMIN_REPLY_PRIVATE_DISABLED=私聊控制功能当前未开启。
QQBOT_ADMIN_REPLY_GROUP_NOT_ALLOWED=这个群不在允许控制范围内。
QQBOT_ADMIN_REPLY_UNKNOWN_COMMAND=没看懂这个控制指令。
QQBOT_ADMIN_REPLY_SUCCESS=已更新群配置。
QQBOT_ADMIN_REPLY_STATUS_PREFIX=当前群配置：
```

真实联调时把这些放在 `scripts/onebot-local.env`。模板见 `scripts/onebot-local.env.example`。

## 权限限制

私聊控制必须同时满足：

- `QQBOT_PRIVATE_ADMIN_COMMAND_ENABLED=true`
- 私聊发送者在 `QQBOT_ADMINS` 中
- 默认只能控制 `QQBOT_ONEBOT_ALLOWED_GROUP_IDS` 中的群

不在 `QQBOT_ADMINS` 中的私聊发送者会被直接忽略，不回复任何内容，避免暴露控制入口。

所有成功修改配置的操作都会写入 `admin_op_log`。

## 指令格式

私聊必须带目标群号：

```text
#群 736566774 状态
#群 736566774 开启机器人
#群 736566774 关闭机器人
#群 736566774 开启表情包
#群 736566774 关闭表情包
#群 736566774 开启被动聊天
#群 736566774 关闭被动聊天
#群 736566774 开启主动插话
#群 736566774 关闭主动插话
#群 736566774 开启知识库
#群 736566774 关闭知识库
#群 736566774 开启表情包知识
#群 736566774 关闭表情包知识
#群 736566774 开启被动知识
#群 736566774 关闭被动知识
#群 736566774 开启聊天知识
#群 736566774 关闭聊天知识
#群 736566774 开启主动知识
#群 736566774 关闭主动知识
#群 736566774 冷却 300
#群 736566774 小时上限 5
#群 736566774 每日上限 30
#群 736566774 人设 你是式部茉优，像普通群友一样自然聊天……
#群 736566774 清空人设
```

## 前端关系

未来前端不要模拟 QQ 私聊指令。

前端应直接调用后端 Admin REST API，并复用 `GroupConfigAdminService` 的配置修改能力。私聊入口只作为手机 QQ 上的应急遥控器，例如前端不可用时临时关闭主动插话。
