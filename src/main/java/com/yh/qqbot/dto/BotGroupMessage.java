package com.yh.qqbot.dto;

import java.time.Instant;

/**
 * 项目内部使用的群消息对象。
 *
 * OneBot 原始 payload 会先被解析成这个对象，再交给后续路由逻辑处理。
 */
public record BotGroupMessage(
        String groupId,                 // 群号，表示消息来自哪个 QQ 群
        String userId,                  // 发送者 QQ 号，表示群里是谁发的消息
        String messageId,               // OneBot 消息 ID，用于日志追踪、问题排查和后续去重
        String rawMessage,              // 原始消息内容，可能包含 OneBot/CQ 码，例如 @ 信息、图片信息等
        String plainText,               // 清洗后的纯文本内容，主要用于关键词判断、路由判断和 AI 输入
        boolean atBot,                  // 是否通过 QQ @ 了机器人
        boolean mentionedBotNickname,   // 是否在文本中提到了机器人昵称，例如“小黄”“黄哥”
        Instant receivedAt              // 消息接收时间，用于日志、统计、冷却和后续扩展
) {

    /**
     * 判断这条群消息是否触发被动聊天。
     *
     * 被 @ 或者文本中提到机器人昵称，都认为用户在主动找机器人说话。
     */
    public boolean triggersPassiveChat() {
        return atBot || mentionedBotNickname;
    }

    /**
     * 返回用于业务判断的有效文本。
     *
     * 避免 plainText 为 null 导致空指针，同时去掉前后空白。
     */
    public String effectiveText() {
        return plainText == null ? "" : plainText.strip();
    }
}