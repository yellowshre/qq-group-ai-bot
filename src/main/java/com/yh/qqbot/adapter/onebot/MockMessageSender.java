package com.yh.qqbot.adapter.onebot;

import com.yh.qqbot.dto.OutboundMessage;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class MockMessageSender implements QqMessageSender {

    private static final Logger log = LoggerFactory.getLogger(MockMessageSender.class);

    @PostConstruct
    public void logSenderType() {
        log.info("QQ message sender active: MockMessageSender");
    }

    @Override
    public boolean sendGroupMessage(String groupId, OutboundMessage outboundMessage) {
        log.info("Mock QQ send success. groupId={}, text={}, imagePath={}",
                groupId, outboundMessage.text(), outboundMessage.imagePath());
        return true;
    }

    @Override
    public boolean sendPrivateMessage(String userId, OutboundMessage outboundMessage) {
        log.info("Mock QQ private send success. userId={}, text={}, imagePath={}",
                userId, outboundMessage.text(), outboundMessage.imagePath());
        return true;
    }
}
