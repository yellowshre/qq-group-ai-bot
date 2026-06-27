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
    private Identity identity = new Identity();
    @NotNull
    private Safety safety = new Safety();
    @NotNull
    private PassiveChat passiveChat = new PassiveChat();
    @NotNull
    private ActiveChat activeChat = new ActiveChat();
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

    public Identity getIdentity() {
        return identity;
    }

    public void setIdentity(Identity identity) {
        this.identity = identity == null ? new Identity() : identity;
    }

    public Safety getSafety() {
        return safety;
    }

    public void setSafety(Safety safety) {
        this.safety = safety == null ? new Safety() : safety;
    }

    public PassiveChat getPassiveChat() {
        return passiveChat;
    }

    public void setPassiveChat(PassiveChat passiveChat) {
        this.passiveChat = passiveChat == null ? new PassiveChat() : passiveChat;
    }

    public ActiveChat getActiveChat() {
        return activeChat;
    }

    public void setActiveChat(ActiveChat activeChat) {
        this.activeChat = activeChat == null ? new ActiveChat() : activeChat;
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

    public static class Identity {
        private String displayName = "\u5c0f\u9ec4";
        private List<String> aliases = new ArrayList<>(List.of("\u5c0f\u9ec4", "\u9ec4\u54e5", "\u673a\u5668\u4eba"));
        private String defaultPersona = BUILT_IN_DEFAULT_PERSONA;

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public List<String> getAliases() {
            return aliases;
        }

        public void setAliases(List<String> aliases) {
            this.aliases = aliases == null ? new ArrayList<>() : new ArrayList<>(aliases);
        }

        public String getDefaultPersona() {
            return defaultPersona;
        }

        public void setDefaultPersona(String defaultPersona) {
            this.defaultPersona = defaultPersona;
        }
    }

    public static class Safety {
        private boolean adminOnly = true;
        private List<String> activeChatOffWords = new ArrayList<>(List.of(
                "#autochatoff",
                "#\u505c\u7528\u4e3b\u52a8\u63d2\u8bdd",
                "#\u522b\u63d2\u8bdd",
                "\u5c0f\u9ec4\u95ed\u5634"
        ));
        private List<String> activeChatOnWords = new ArrayList<>(List.of(
                "#autochaton",
                "#\u5f00\u542f\u4e3b\u52a8\u63d2\u8bdd",
                "#\u53ef\u4ee5\u8bf4\u8bdd\u4e86",
                "\u5c0f\u9ec4\u8bf4\u8bdd"
        ));

        public boolean isAdminOnly() {
            return adminOnly;
        }

        public void setAdminOnly(boolean adminOnly) {
            this.adminOnly = adminOnly;
        }

        public List<String> getActiveChatOffWords() {
            return activeChatOffWords;
        }

        public void setActiveChatOffWords(List<String> activeChatOffWords) {
            this.activeChatOffWords = activeChatOffWords == null ? new ArrayList<>() : new ArrayList<>(activeChatOffWords);
        }

        public List<String> getActiveChatOnWords() {
            return activeChatOnWords;
        }

        public void setActiveChatOnWords(List<String> activeChatOnWords) {
            this.activeChatOnWords = activeChatOnWords == null ? new ArrayList<>() : new ArrayList<>(activeChatOnWords);
        }
    }

    public static class PassiveChat {
        private List<String> triggerWords = new ArrayList<>(List.of("\u5c0f\u9ec4", "\u9ec4\u54e5", "\u673a\u5668\u4eba"));

        public List<String> getTriggerWords() {
            return triggerWords;
        }

        public void setTriggerWords(List<String> triggerWords) {
            this.triggerWords = triggerWords == null ? new ArrayList<>() : new ArrayList<>(triggerWords);
        }
    }

    public static class ActiveChat {
        private boolean enabled = true;
        @Min(1)
        private long cooldownSeconds = 180;
        @Min(0)
        private long maxPerHour = 20;
        private double randomProbability = 1.0;
        private double minConfidence = 0.6;
        @Min(1)
        private int minMessageLength = 3;
        @Min(1)
        private int maxMessageLength = 80;
        private boolean allowAfterMemeSent = false;
        private boolean allowAfterBotMessage = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getCooldownSeconds() {
            return cooldownSeconds;
        }

        public void setCooldownSeconds(long cooldownSeconds) {
            this.cooldownSeconds = cooldownSeconds;
        }

        public long getMaxPerHour() {
            return maxPerHour;
        }

        public void setMaxPerHour(long maxPerHour) {
            this.maxPerHour = maxPerHour;
        }

        public double getRandomProbability() {
            return randomProbability;
        }

        public void setRandomProbability(double randomProbability) {
            this.randomProbability = randomProbability;
        }

        public double getMinConfidence() {
            return minConfidence;
        }

        public void setMinConfidence(double minConfidence) {
            this.minConfidence = minConfidence;
        }

        public int getMinMessageLength() {
            return minMessageLength;
        }

        public void setMinMessageLength(int minMessageLength) {
            this.minMessageLength = minMessageLength;
        }

        public int getMaxMessageLength() {
            return maxMessageLength;
        }

        public void setMaxMessageLength(int maxMessageLength) {
            this.maxMessageLength = maxMessageLength;
        }

        public boolean isAllowAfterMemeSent() {
            return allowAfterMemeSent;
        }

        public void setAllowAfterMemeSent(boolean allowAfterMemeSent) {
            this.allowAfterMemeSent = allowAfterMemeSent;
        }

        public boolean isAllowAfterBotMessage() {
            return allowAfterBotMessage;
        }

        public void setAllowAfterBotMessage(boolean allowAfterBotMessage) {
            this.allowAfterBotMessage = allowAfterBotMessage;
        }
    }

    public static class OneBot {
        private String apiServerHost = "http://127.0.0.1:3000";
        private String eventServerHost = "ws://127.0.0.1:3001";
        private String accessToken = "";
        private String selfId = "";
        private List<String> allowedGroupIds = new ArrayList<>();
        private boolean dryRun = true;
        @NotNull
        private Ws ws = new Ws();

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

        public String getSelfId() {
            return selfId;
        }

        public void setSelfId(String selfId) {
            this.selfId = selfId;
        }

        public List<String> getAllowedGroupIds() {
            return allowedGroupIds;
        }

        public void setAllowedGroupIds(List<String> allowedGroupIds) {
            this.allowedGroupIds = allowedGroupIds == null ? new ArrayList<>() : new ArrayList<>(allowedGroupIds);
        }

        public boolean isDryRun() {
            return dryRun;
        }

        public void setDryRun(boolean dryRun) {
            this.dryRun = dryRun;
        }

        public Ws getWs() {
            return ws;
        }

        public void setWs(Ws ws) {
            this.ws = ws == null ? new Ws() : ws;
        }
    }

    public static class Ws {
        private boolean enabled = false;
        private String url = "ws://127.0.0.1:3001/";
        private String accessToken = "";
        @Min(1)
        private long reconnectDelayMs = 5000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        public long getReconnectDelayMs() {
            return reconnectDelayMs;
        }

        public void setReconnectDelayMs(long reconnectDelayMs) {
            this.reconnectDelayMs = reconnectDelayMs;
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
        private String baseDir = "./memes";

        public boolean isCachePreheatEnabled() {
            return cachePreheatEnabled;
        }

        public void setCachePreheatEnabled(boolean cachePreheatEnabled) {
            this.cachePreheatEnabled = cachePreheatEnabled;
        }

        public String getBaseDir() {
            return baseDir;
        }

        public void setBaseDir(String baseDir) {
            this.baseDir = baseDir;
        }
    }

    public static class Dify {
        private boolean enabled = false;
        private String baseUrl = "http://127.0.0.1:5001";
        private String memeSceneApiKey = "";
        private String passiveChatApiKey = "";
        private String activeChatApiKey = "";
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

        public String getMemeSceneApiKey() {
            return memeSceneApiKey;
        }

        public void setMemeSceneApiKey(String memeSceneApiKey) {
            this.memeSceneApiKey = memeSceneApiKey;
        }

        public String getPassiveChatApiKey() {
            return passiveChatApiKey;
        }

        public void setPassiveChatApiKey(String passiveChatApiKey) {
            this.passiveChatApiKey = passiveChatApiKey;
        }

        public String getSceneWorkflowId() {
            return workflow.getMemeScene();
        }

        public String getActiveWorkflowId() {
            return workflow.getActiveChat();
        }

        public String getActiveChatApiKey() {
            return activeChatApiKey;
        }

        public void setActiveChatApiKey(String activeChatApiKey) {
            this.activeChatApiKey = activeChatApiKey;
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
    }

    public static class Workflow {
        private String memeScene = "";
        private String passiveChat = "";
        private String activeChat = "";

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

        public String getActiveChat() {
            return activeChat;
        }

        public void setActiveChat(String activeChat) {
            this.activeChat = activeChat;
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
