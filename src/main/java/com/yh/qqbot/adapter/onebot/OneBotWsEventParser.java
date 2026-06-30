package com.yh.qqbot.adapter.onebot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yh.qqbot.config.properties.QqBotProperties;
import com.yh.qqbot.dto.BotGroupMessage;
import com.yh.qqbot.dto.BotPrivateMessage;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * OneBot WebSocket 事件解析器。
 *
 * 负责把 OneBot 推送过来的原始 JSON payload 解析成项目内部 DTO：
 * 群消息 -> BotGroupMessage
 * 私聊消息 -> BotPrivateMessage
 * 无需处理的事件 -> ParseResult.ignored(...)
 */
@Component
public class OneBotWsEventParser {

    /**
     * 机器人配置项，用于读取白名单群、机器人 QQ、自定义昵称、触发词等。
     */
    private final QqBotProperties properties;

    /**
     * Jackson JSON 解析器，用于把 payload 字符串转成 JsonNode。
     */
    private final ObjectMapper objectMapper;

    public OneBotWsEventParser(QqBotProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * 兼容旧调用：只关心群消息时使用。
     */
    public Optional<BotGroupMessage> parse(String payload) {
        return parseDetailed(payload).message();
    }

    /**
     * 只关心私聊消息时使用。
     */
    public Optional<BotPrivateMessage> parsePrivate(String payload) {
        return parseDetailed(payload).privateMessage();
    }

    /**
     * 字符串 payload 的主入口。
     *
     * 先判断空内容，再尝试解析 JSON。
     * 如果解析失败，不抛异常，而是返回 ignored 结果，避免 WebSocket 处理线程被打断。
     */
    public ParseResult parseDetailed(String payload) {
        if (payload == null || payload.isBlank()) {
            return ParseResult.ignored("EMPTY_PAYLOAD", "", "", "", "", "");
        }
        try {
            return parseDetailed(objectMapper.readTree(payload));
        } catch (Exception ex) {
            return ParseResult.ignored("JSON_PARSE_FAILED", "", "", "", "", "");
        }
    }

    /**
     * 兼容旧调用：传入 JsonNode，只返回群消息。
     */
    public Optional<BotGroupMessage> parse(JsonNode root) {
        return parseDetailed(root).message();
    }

    /**
     * JSON 解析后的核心处理方法。
     *
     * 这里会完成事件过滤、群聊/私聊分流、自身消息过滤、白名单群过滤，
     * 最后构造 BotGroupMessage 或 BotPrivateMessage。
     */
    public ParseResult parseDetailed(JsonNode root) {
        if (root == null || !root.isObject()) {
            return ParseResult.ignored("JSON_NOT_OBJECT", "", "", "", "", "");
        }

        String postType = text(root, "post_type");
        String messageType = text(root, "message_type");
        String groupId = text(root, "group_id");
        String userId = text(root, "user_id");
        String rawMessage = text(root, "raw_message");

        // OneBot 主动发送消息后的响应，不是用户消息，需要忽略。
        if (isActionResponse(root)) {
            return ignored("ACTION_RESPONSE", root, rawMessage);
        }

        // 心跳事件只表示连接存活，不进入消息路由。
        if ("meta_event".equals(postType) && "heartbeat".equals(text(root, "meta_event_type"))) {
            return ignored("HEARTBEAT_EVENT", root, rawMessage);
        }

        // 生命周期事件只表示连接、启停等状态变化，不进入消息路由。
        if ("meta_event".equals(postType) && "lifecycle".equals(text(root, "meta_event_type"))) {
            return ignored("LIFECYCLE_EVENT", root, rawMessage);
        }

        // 当前解析器只处理 message 类型事件。
        if (!"message".equals(postType)) {
            return ignored("POST_TYPE_NOT_MESSAGE", root, rawMessage);
        }

        // 私聊消息走单独分支，主要用于管理员私聊指令。
        if ("private".equals(messageType)) {
            return parsePrivateMessage(root, postType, messageType, userId, rawMessage);
        }

        // 除私聊外，只处理群消息，其他 message_type 暂时忽略。
        if (!"group".equals(messageType)) {
            return ignored("MESSAGE_TYPE_NOT_GROUP", root, rawMessage);
        }

        // 群消息必须来自配置允许的群，避免机器人响应非目标群。
        if (!isAllowedGroup(groupId)) {
            return ignored("GROUP_NOT_ALLOWED", root, rawMessage);
        }

        String selfId = configuredSelfId(root);

        // 忽略机器人自己发出的消息，防止自己回复自己造成循环。
        if (hasText(selfId) && selfId.equals(userId)) {
            return ignored("SELF_MESSAGE", root, rawMessage);
        }

        MessageParts parts = messageParts(root.path("message"), selfId);
        String plainText = routedText(parts, rawMessage);

        // 没有有效文本的消息不进入后续路由。
        if (!hasText(plainText)) {
            return ignored("TEXT_EMPTY", root, rawMessage);
        }

        // 有些 OneBot 实现可能 raw_message 为空，这里做一个兜底。
        if (!hasText(rawMessage)) {
            rawMessage = parts.atBot() && hasText(selfId) ? "@" + selfId + " " + parts.text() : parts.text();
        }

        return ParseResult.routableGroup(new BotGroupMessage(
                groupId,
                userId,
                text(root, "message_id"),
                rawMessage,
                plainText,
                parts.atBot(),
                mentionedBotNickname(plainText),
                receivedAt(root)
        ), postType, messageType, groupId, userId, rawMessage);
    }

    /**
     * 解析私聊消息。
     *
     * 私聊不需要判断群白名单，也不需要判断是否 @ 机器人。
     */
    private ParseResult parsePrivateMessage(
            JsonNode root,
            String postType,
            String messageType,
            String userId,
            String rawMessage) {
        String selfId = configuredSelfId(root);

        // 忽略机器人自己发给自己的私聊消息。
        if (hasText(selfId) && selfId.equals(userId)) {
            return ignored("SELF_MESSAGE", root, rawMessage);
        }

        MessageParts parts = messageParts(root.path("message"), selfId);
        String plainText = hasText(parts.text()) ? parts.text() : rawMessage;

        if (!hasText(plainText)) {
            return ignored("TEXT_EMPTY", root, rawMessage);
        }

        if (!hasText(rawMessage)) {
            rawMessage = plainText;
        }

        return ParseResult.routablePrivate(new BotPrivateMessage(
                userId,
                text(root, "message_id"),
                rawMessage,
                plainText.strip(),
                receivedAt(root)
        ), postType, messageType, "", userId, rawMessage);
    }

    /**
     * 判断群号是否在允许列表中。
     *
     * allowedGroupIds 支持配置多个值，也支持单个配置项里用逗号分隔多个群号。
     */
    private boolean isAllowedGroup(String groupId) {
        if (!hasText(groupId)) {
            return false;
        }
        Set<String> allowed = new HashSet<>();
        for (String value : properties.getOnebot().getAllowedGroupIds()) {
            if (!hasText(value)) {
                continue;
            }
            for (String part : value.split(",")) {
                if (hasText(part)) {
                    allowed.add(part.strip());
                }
            }
        }
        return !allowed.isEmpty() && allowed.contains(groupId);
    }

    /**
     * 从 OneBot message 数组中提取文本和 @ 机器人状态。
     *
     * OneBot 的 message 通常是分段数组，例如：
     * text 段表示普通文本；
     * at 段表示 @ 某个 QQ。
     */
    private MessageParts messageParts(JsonNode messageNode, String selfId) {
        if (!messageNode.isArray()) {
            return new MessageParts("", false);
        }
        StringBuilder text = new StringBuilder();
        boolean atBot = false;
        for (JsonNode segment : messageNode) {
            String type = text(segment, "type");
            JsonNode data = segment.path("data");
            if ("text".equals(type)) {
                text.append(text(data, "text"));
            } else if ("at".equals(type) && hasText(selfId) && selfId.equals(text(data, "qq"))) {
                atBot = true;
            }
        }
        return new MessageParts(text.toString().strip(), atBot);
    }

    /**
     * 得到真正用于后续路由的文本。
     *
     * 如果用户 @ 了机器人，会人为补上一个“@机器人名”的前缀，
     * 让后续被动聊天逻辑更容易识别这是在叫机器人。
     */
    private String routedText(MessageParts parts, String rawMessage) {
        String text = hasText(parts.text()) ? parts.text() : rawMessage;
        if (!hasText(text)) {
            return "";
        }
        if (parts.atBot()) {
            return "@" + botMentionLabel() + " " + text.strip();
        }
        return text;
    }

    /**
     * 获取机器人被 @ 时展示用的名称。
     *
     * 优先使用 displayName，其次使用 nicknames，最后兜底为“机器人”。
     */
    private String botMentionLabel() {
        String displayName = properties.getIdentity().getDisplayName();
        if (hasText(displayName)) {
            return displayName.strip();
        }
        for (String nickname : properties.getNicknames()) {
            if (hasText(nickname)) {
                return nickname.strip();
            }
        }
        return "机器人";
    }

    /**
     * 判断当前 payload 是否是 OneBot action 响应。
     *
     * action 响应通常带 echo，或者同时带 status 和 retcode。
     */
    private boolean isActionResponse(JsonNode root) {
        return root.has("echo") || (root.has("status") && root.has("retcode"));
    }

    /**
     * 构造 ignored 类型 ParseResult，同时保留一些日志排查字段。
     */
    private ParseResult ignored(String reason, JsonNode root, String rawMessage) {
        return ParseResult.ignored(
                reason,
                text(root, "post_type"),
                text(root, "message_type"),
                text(root, "group_id"),
                text(root, "user_id"),
                rawMessage
        );
    }

    /**
     * 获取机器人自己的 QQ 号。
     *
     * 优先使用配置文件中的 selfId；
     * 如果配置为空，再尝试使用 OneBot payload 里的 self_id。
     */
    private String configuredSelfId(JsonNode root) {
        String configured = properties.getOnebot().getSelfId();
        if (hasText(configured)) {
            return configured.strip();
        }
        return text(root, "self_id");
    }

    /**
     * 判断文本中是否提到了机器人昵称或触发词。
     */
    private boolean mentionedBotNickname(String text) {
        if (!hasText(text)) {
            return false;
        }
        return containsAny(text, properties.getPassiveChat().getTriggerWords())
                || containsAny(text, properties.getIdentity().getAliases())
                || containsAny(text, properties.getNicknames());
    }

    /**
     * 判断 text 是否包含 words 中任意一个有效词。
     */
    private boolean containsAny(String text, List<String> words) {
        if (words == null || words.isEmpty()) {
            return false;
        }
        for (String word : words) {
            if (hasText(word) && text.contains(word.strip())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取消息接收时间。
     *
     * OneBot 的 time 通常是秒级时间戳；
     * 如果没有 time，则使用当前时间兜底。
     */
    private Instant receivedAt(JsonNode root) {
        JsonNode time = root.get("time");
        if (time != null && time.canConvertToLong()) {
            return Instant.ofEpochSecond(time.asLong());
        }
        return Instant.now();
    }

    /**
     * 安全读取 JsonNode 中的文本字段。
     *
     * 字段不存在、为 null、节点异常时，统一返回空字符串。
     */
    private String text(JsonNode node, String fieldName) {
        if (node == null || node.isMissingNode() || node.get(fieldName) == null || node.get(fieldName).isNull()) {
            return "";
        }
        return node.get(fieldName).asText("");
    }

    /**
     * 判断字符串是否有有效内容。
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 从 OneBot message 数组中提取出来的中间结果。
     *
     * @param text 文本段拼接后的内容
     * @param atBot 是否 @ 了机器人自己
     */
    private record MessageParts(String text, boolean atBot) {
    }

    /**
     * 解析结果。
     *
     * 三种可能：
     * 1. message 有值：可路由的群消息；
     * 2. privateMessage 有值：可处理的私聊消息；
     * 3. 两者都为空：该 payload 被忽略，原因记录在 ignoredReason。
     */
    public record ParseResult(
            Optional<BotGroupMessage> message,
            Optional<BotPrivateMessage> privateMessage,
            String ignoredReason,
            String postType,
            String messageType,
            String groupId,
            String userId,
            String rawMessage
    ) {

        /**
         * 构造可路由的群消息解析结果。
         */
        private static ParseResult routableGroup(
                BotGroupMessage message,
                String postType,
                String messageType,
                String groupId,
                String userId,
                String rawMessage) {
            return new ParseResult(Optional.of(message), Optional.empty(), "", postType, messageType, groupId, userId, rawMessage);
        }

        /**
         * 构造可处理的私聊消息解析结果。
         */
        private static ParseResult routablePrivate(
                BotPrivateMessage message,
                String postType,
                String messageType,
                String groupId,
                String userId,
                String rawMessage) {
            return new ParseResult(Optional.empty(), Optional.of(message), "", postType, messageType, groupId, userId, rawMessage);
        }

        /**
         * 构造被忽略的解析结果。
         */
        private static ParseResult ignored(
                String reason,
                String postType,
                String messageType,
                String groupId,
                String userId,
                String rawMessage) {
            return new ParseResult(Optional.empty(), Optional.empty(), reason, postType, messageType, groupId, userId, rawMessage);
        }
    }
}