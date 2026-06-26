package com.yh.qqbot.cache;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class RedisKeys {

    private RedisKeys() {
    }

    public static String groupConfig(String groupId) {
        return "config:group:" + groupId;
    }

    public static String messageDedup(String messageId) {
        return "msg:dedup:" + messageId;
    }

    public static String chatContext(String groupId) {
        return "chat:ctx:" + groupId;
    }

    public static String recentChat(String groupId) {
        return "chat:recent:" + groupId;
    }

    public static String memeKeyword(String keyword) {
        return "meme:kw:" + keyword;
    }

    public static String memeScene(String sceneCode) {
        return "meme:scene:" + sceneCode;
    }

    public static String memeSemanticCache(String text) {
        return "meme:cache:" + sha256(text);
    }

    public static String emojiRate(String groupId) {
        return "rate:emoji:" + groupId;
    }

    public static String passiveChatRate(String groupId) {
        return "rate:chat:passive:" + groupId;
    }

    public static String activeChatRate(String groupId) {
        return "rate:chat:active:" + groupId;
    }

    public static String activeDecision(String fingerprint) {
        return "decision:active:" + fingerprint;
    }

    public static String tempMedia(String messageId) {
        return "media:tmp:" + messageId;
    }

    private static String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
