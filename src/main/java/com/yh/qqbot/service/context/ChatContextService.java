package com.yh.qqbot.service.context;

import com.yh.qqbot.enums.MemoryMode;
import java.util.List;

public interface ChatContextService {

    List<String> loadHotContext(String groupId);

    List<String> loadColdSummaries(String groupId);

    List<String> recentMessagesForActiveDecision(String groupId);

    void appendTurn(String groupId, String userText, String assistantText, MemoryMode memoryMode);

    void rememberRecentMessage(String groupId, String userText);

    void clearGroupMemory(String groupId);
}
