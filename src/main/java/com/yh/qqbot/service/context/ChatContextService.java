package com.yh.qqbot.service.context;

import com.yh.qqbot.enums.MemoryMode;
import java.util.List;

public interface ChatContextService {

    String NO_CONTEXT = "\u6682\u65e0\u4e0a\u4e0b\u6587";

    String getRecentMessages(Long groupId);

    void appendUserMessage(Long groupId, Long userId, String text);

    void appendBotReply(Long groupId, String botName, String replyText);

    void appendMessage(Long groupId, String roleName, String content);

    List<String> loadHotContext(String groupId);

    List<String> loadColdSummaries(String groupId);

    List<String> recentMessagesForActiveDecision(String groupId);

    void appendTurn(String groupId, String userText, String assistantText, MemoryMode memoryMode);

    void rememberRecentMessage(String groupId, String userText);

    void clearGroupMemory(String groupId);
}
