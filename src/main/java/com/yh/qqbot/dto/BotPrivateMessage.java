package com.yh.qqbot.dto;

import java.time.Instant;

/**
 * 项目内部使用的私聊消息对象。
 *
 * 私聊消息主要用于管理员私聊控制群配置、查询排行等指令场景。
 */
public record BotPrivateMessage(
        String userId,                  // 私聊发送者 QQ 号
        String messageId,               // OneBot 消息 ID，用于日志追踪、问题排查和后续去重
        String rawMessage,              // 原始私聊消息内容
        String effectiveText,           // 清洗后的有效文本，主要用于私聊管理员指令解析
        Instant receivedAt              // 消息接收时间，用于日志、审计和后续扩展
) {
}