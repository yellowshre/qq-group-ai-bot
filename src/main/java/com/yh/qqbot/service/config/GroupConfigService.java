package com.yh.qqbot.service.config;

import com.yh.qqbot.dto.GroupConfigSnapshot;
import java.util.function.UnaryOperator;

public interface GroupConfigService {

    GroupConfigSnapshot getConfig(String groupId);

    GroupConfigSnapshot updateConfig(String groupId, UnaryOperator<GroupConfigSnapshot> updater);
}
