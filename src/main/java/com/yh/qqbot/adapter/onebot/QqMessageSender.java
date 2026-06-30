package com.yh.qqbot.adapter.onebot;

import com.yh.qqbot.dto.OutboundMessage;

/**
 * QQ 消息发送统一接口。
 *
 * 上层业务只关心“发群消息”或“发私聊消息”，不关心底层到底是
 * WebSocket、HTTP，还是 dev 环境下的 Mock 实现。
 */
public interface QqMessageSender {

    /**
     * 向指定 QQ 群发送消息。
     *
     * @param groupId 群号
     * @param outboundMessage 要发送的消息内容，可以是文本、图片或图文组合
     * @return true 表示发送请求成功提交；false 表示未成功发送
     */
    boolean sendGroupMessage(String groupId, OutboundMessage outboundMessage);

    /**
     * 向指定 QQ 用户发送私聊消息。
     *
     * @param userId 用户 QQ 号
     * @param outboundMessage 要发送的私聊消息内容
     * @return true 表示发送请求成功提交；false 表示未成功发送
     */
    boolean sendPrivateMessage(String userId, OutboundMessage outboundMessage);
}