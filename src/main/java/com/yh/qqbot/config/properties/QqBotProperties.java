package com.yh.qqbot.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "qqbot")
public class QqBotProperties {

    private static final String BUILT_IN_DEFAULT_PERSONA =
            "\u4f60\u662f\u4e00\u4e2a\u8bf4\u8bdd\u7b80\u77ed\u3001\u81ea\u7136\u3001\u7565\u5e26\u5410\u69fd\u4f46\u4e0d\u6076\u610f\u653b\u51fb\u4eba\u7684 QQ \u7fa4\u673a\u5668\u4eba\u3002";

    private String botId = "";
    private List<String> nicknames = new ArrayList<>();
    private List<String> admins = new ArrayList<>();
    private String defaultPersona = BUILT_IN_DEFAULT_PERSONA;
    private String defaultSafeReply = "OK, I will stay quiet for a while.";
    @NotNull
    private OneBot onebot = new OneBot();
    @NotNull
    private Redis redis = new Redis();
    @NotNull
    private RateLimit rateLimit = new RateLimit();
    @NotNull
    private Memory memory = new Memory();
    @NotNull
    private ChatContext chatContext = new ChatContext();
    @NotNull
    private Meme meme = new Meme();
    @NotNull
    private Dify dify = new Dify();
    @NotNull
    private Async async = new Async();

    public String getBotId() {
        return botId;
    }

    public void setBotId(String botId) {
        this.botId = botId;
    }

    public List<String> getNicknames() {
        return nicknames;
    }

    public void setNicknames(List<String> nicknames) {
        this.nicknames = nicknames;
    }

    public List<String> getAdmins() {
        return admins;
    }

    public void setAdmins(List<String> admins) {
        this.admins = admins;
    }

    public String getDefaultPersona() {
        return defaultPersona;
    }

    public void setDefaultPersona(String defaultPersona) {
        this.defaultPersona = defaultPersona;
    }

    public String getDefaultSafeReply() {
        return defaultSafeReply;
    }

    public void setDefaultSafeReply(String defaultSafeReply) {
        this.defaultSafeReply = defaultSafeReply;
    }

    public OneBot getOnebot() {
        return onebot;
    }

    public void setOnebot(OneBot onebot) {
        this.onebot = onebot;
    }

    public Redis getRedis() {
        return redis;
    }

    public void setRedis(Redis redis) {
        this.redis = redis;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimit rateLimit) {
        this.rateLimit = rateLimit;
    }

    public Memory getMemory() {
        return memory;
    }

    public void setMemory(Memory memory) {
        this.memory = memory;
    }

    public ChatContext getChatContext() {
        return chatContext;
    }

    public void setChatContext(ChatContext chatContext) {
        this.chatContext = chatContext == null ? new ChatContext() : chatContext;
    }

    public Meme getMeme() {
        return meme;
    }

    public void setMeme(Meme meme) {
        this.meme = meme;
    }

    public Dify getDify() {
        return dify;
    }

    public void setDify(Dify dify) {
        this.dify = dify;
    }

    public Async getAsync() {
        return async;
    }

    public void setAsync(Async async) {
        this.async = async;
    }

    public static class OneBot {
        private String apiServerHost = "http://127.0.0.1:3000";
        private String eventServerHost = "ws://127.0.0.1:3001";
        private String accessToken = "";
        private boolean dryRun = true;

        public String getApiServerHost() {
            return apiServerHost;
        }

        public void setApiServerHost(String apiServerHost) {
            this.apiServerHost = apiServerHost;
        }

        public String getEventServerHost() {
            return eventServerHost;
        }

        public void setEventServerHost(String eventServerHost) {
            this.eventServerHost = eventServerHost;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        public boolean isDryRun() {
            return dryRun;
        }

        public void setDryRun(boolean dryRun) {
            this.dryRun = dryRun;
        }
    }

    public static class Redis {
        private String host = "127.0.0.1";
        @Min(1)
        private int port = 6379;
        private String password = "";
        private Duration timeout = Duration.ofSeconds(3);
        @NotNull
        private Database database = new Database();

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public Database getDatabase() {
            return database;
        }

        public void setDatabase(Database database) {
            this.database = database;
        }
    }

    public static class Database {
        private int base = 0;
        private int chat = 1;
        private int meme = 2;
        private int rate = 3;

        public int getBase() {
            return base;
        }

        public void setBase(int base) {
            this.base = base;
        }

        public int getChat() {
            return chat;
        }

        public void setChat(int chat) {
            this.chat = chat;
        }

        public int getMeme() {
            return meme;
        }

        public void setMeme(int meme) {
            this.meme = meme;
        }

        public int getRate() {
            return rate;
        }

        public void setRate(int rate) {
            this.rate = rate;
        }
    }

    public static class RateLimit {
        @Min(0)
        private int emojiPerMinute = 6;
        @Min(0)
        private int passiveChatPerMinute = 3;
        @Min(0)
        private int activeChatPerMinute = 1;

        public int getEmojiPerMinute() {
            return emojiPerMinute;
        }

        public void setEmojiPerMinute(int emojiPerMinute) {
            this.emojiPerMinute = emojiPerMinute;
        }

        public int getPassiveChatPerMinute() {
            return passiveChatPerMinute;
        }

        public void setPassiveChatPerMinute(int passiveChatPerMinute) {
            this.passiveChatPerMinute = passiveChatPerMinute;
        }

        public int getActiveChatPerMinute() {
            return activeChatPerMinute;
        }

        public void setActiveChatPerMinute(int activeChatPerMinute) {
            this.activeChatPerMinute = activeChatPerMinute;
        }
    }

    public static class Memory {
        @Min(1)
        private int shortTurns = 10;
        @Min(1)
        private int longTurns = 20;
        private Duration ttl = Duration.ofMinutes(30);

        public int getShortTurns() {
            return shortTurns;
        }

        public void setShortTurns(int shortTurns) {
            this.shortTurns = shortTurns;
        }

        public int getLongTurns() {
            return longTurns;
        }

        public void setLongTurns(int longTurns) {
            this.longTurns = longTurns;
        }

        public Duration getTtl() {
            return ttl;
        }

        public void setTtl(Duration ttl) {
            this.ttl = ttl;
        }
    }

    public static class ChatContext {
        private String keyPrefix = "qqbot:chat:ctx:";
        @Min(1)
        private int maxSize = 10;
        @Min(1)
        private int ttlMinutes = 120;
        @Min(1)
        private int maxMessageLength = 160;

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }

        public int getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(int maxSize) {
            this.maxSize = maxSize;
        }

        public int getTtlMinutes() {
            return ttlMinutes;
        }

        public void setTtlMinutes(int ttlMinutes) {
            this.ttlMinutes = ttlMinutes;
        }

        public int getMaxMessageLength() {
            return maxMessageLength;
        }

        public void setMaxMessageLength(int maxMessageLength) {
            this.maxMessageLength = maxMessageLength;
        }
    }

    public static class Meme {
        private boolean cachePreheatEnabled = true;

        public boolean isCachePreheatEnabled() {
            return cachePreheatEnabled;
        }

        public void setCachePreheatEnabled(boolean cachePreheatEnabled) {
            this.cachePreheatEnabled = cachePreheatEnabled;
        }
    }

    public static class Dify {
        private boolean enabled = false;
        private String baseUrl = "http://127.0.0.1:5001";
        private String apiKey = "";
        private String sceneWorkflowId = "";
        private String chatWorkflowId = "";
        private String activeWorkflowId = "";
        @NotNull
        private Workflow workflow = new Workflow();
        @Min(1)
        private long timeoutMs = 30_000;
        private double passiveChatMinConfidence = 0.5;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getSceneWorkflowId() {
            return hasText(sceneWorkflowId) ? sceneWorkflowId : workflow.getMemeScene();
        }

        public void setSceneWorkflowId(String sceneWorkflowId) {
            this.sceneWorkflowId = sceneWorkflowId;
        }

        public String getChatWorkflowId() {
            return hasText(chatWorkflowId) ? chatWorkflowId : workflow.getChat();
        }

        public void setChatWorkflowId(String chatWorkflowId) {
            this.chatWorkflowId = chatWorkflowId;
        }

        public String getActiveWorkflowId() {
            return hasText(activeWorkflowId) ? activeWorkflowId : workflow.getActive();
        }

        public void setActiveWorkflowId(String activeWorkflowId) {
            this.activeWorkflowId = activeWorkflowId;
        }

        public String getPassiveChatWorkflowId() {
            return workflow.getPassiveChat();
        }

        public Workflow getWorkflow() {
            return workflow;
        }

        public void setWorkflow(Workflow workflow) {
            this.workflow = workflow == null ? new Workflow() : workflow;
        }

        public Duration getTimeout() {
            return Duration.ofMillis(timeoutMs);
        }

        public void setTimeout(Duration timeout) {
            this.timeoutMs = timeout == null ? 30_000 : timeout.toMillis();
        }

        public long getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
        }

        public double getPassiveChatMinConfidence() {
            return passiveChatMinConfidence;
        }

        public void setPassiveChatMinConfidence(double passiveChatMinConfidence) {
            this.passiveChatMinConfidence = passiveChatMinConfidence;
        }

        private boolean hasText(String value) {
            return value != null && !value.isBlank();
        }
    }

    public static class Workflow {
        private String memeScene = "";
        private String passiveChat = "";
        private String chat = "";
        private String active = "";

        public String getMemeScene() {
            return memeScene;
        }

        public void setMemeScene(String memeScene) {
            this.memeScene = memeScene;
        }

        public String getPassiveChat() {
            return passiveChat;
        }

        public void setPassiveChat(String passiveChat) {
            this.passiveChat = passiveChat;
        }

        public String getChat() {
            return chat;
        }

        public void setChat(String chat) {
            this.chat = chat;
        }

        public String getActive() {
            return active;
        }

        public void setActive(String active) {
            this.active = active;
        }
    }

    public static class Async {
        @Min(1)
        private int corePoolSize = 4;
        @Min(1)
        private int maxPoolSize = 12;
        @Min(1)
        private int queueCapacity = 500;

        public int getCorePoolSize() {
            return corePoolSize;
        }

        public void setCorePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
        }

        public int getMaxPoolSize() {
            return maxPoolSize;
        }

        public void setMaxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }
    }
}
