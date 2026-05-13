package com.aidevops.system.service.impl;

import com.aidevops.system.config.AiChatProperties;
import com.aidevops.system.service.IAiChatService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AiChatServiceImpl implements IAiChatService {

    private final AiChatProperties properties;
    private final OpenClawGatewayClient gatewayClient;
    private final Map<String, Map<String, Object>> sessionStore = new ConcurrentHashMap<>();
    private final Map<String, List<Map<String, Object>>> messageStore = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> pendingRuns = new ConcurrentHashMap<>();

    public AiChatServiceImpl(AiChatProperties properties, OpenClawGatewayClient gatewayClient) {
        this.properties = properties;
        this.gatewayClient = gatewayClient;
        initDefaultSession();
    }

    private void initDefaultSession() {
        sessionStore.computeIfAbsent("chat_demo_main", key -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("sessionId", "chat_demo_main");
            row.put("title", "AI运维对话");
            row.put("scene", properties.getDefaultScene());
            row.put("lastMessage", "当前已接入独立 OpenClaw 会话与真实 Gateway 通道。");
            row.put("createTime", new Date());
            row.put("updateTime", new Date());
            row.put("openclawSessionKey", "aidevops-chat:chat_demo_main");
            row.put("status", gatewayClient.enabled() ? "gateway-probe" : "mock");
            return row;
        });

        messageStore.computeIfAbsent("chat_demo_main", key -> new ArrayList<>());
    }

    @Override
    public Map<String, Object> createSession(Map<String, Object> req) {
        String sessionId = "chat_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String title = valueOrDefault(req, "title", "新会话");
        String scene = valueOrDefault(req, "scene", properties.getDefaultScene());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sessionId", sessionId);
        data.put("title", title);
        data.put("scene", scene);
        data.put("createTime", new Date());
        data.put("updateTime", new Date());
        data.put("status", gatewayClient.enabled() ? "gateway-probe" : "mock");
        data.put("lastMessage", "已创建会话，等待发送第一条消息。");
        data.put("openclawSessionKey", "aidevops-chat:" + sessionId);
        sessionStore.put(sessionId, data);

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(buildMessage("assistant", gatewayClient.enabled()
            ? "新会话已建立。当前系统会先探测 OpenClaw Gateway 的 WS challenge，再继续推进真实 connect。"
            : "新会话已建立。当前仍使用本地回退模式，后续接入 OpenClaw Gateway。"
        ));
        messageStore.put(sessionId, messages);
        return data;
    }

    @Override
    public List<Map<String, Object>> listSessions() {
        initDefaultSession();
        List<Map<String, Object>> rows = new ArrayList<>(sessionStore.values());
        rows.sort(Comparator.comparing(item -> (Date) item.getOrDefault("updateTime", new Date()), Comparator.reverseOrder()));
        return rows;
    }

    @Override
    public Map<String, Object> getHistory(String sessionId) {
        initDefaultSession();
        String actualSessionId = sessionStore.containsKey(sessionId) ? sessionId : "chat_demo_main";
        Map<String, Object> session = sessionStore.get(actualSessionId);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sessionId", actualSessionId);
        data.put("title", session.get("title"));
        data.put("status", session.get("status"));
        data.put("diagnostics", gatewayClient.diagnostics());
        data.put("messages", new ArrayList<>(messageStore.getOrDefault(actualSessionId, new ArrayList<>())));
        return data;
    }

    @Override
    public Map<String, Object> sendMessage(Map<String, Object> req) {
        initDefaultSession();
        String sessionId = valueOrDefault(req, "sessionId", "chat_demo_main");
        String message = valueOrDefault(req, "message", "");
        if (!sessionStore.containsKey(sessionId)) {
            sessionId = "chat_demo_main";
        }

        List<Map<String, Object>> messages = messageStore.computeIfAbsent(sessionId, key -> new ArrayList<>());
        messages.add(buildMessage("user", message));

        Map<String, Object> session = sessionStore.get(sessionId);
        session.put("lastMessage", message);
        session.put("updateTime", new Date());
        session.put("status", gatewayClient.enabled() ? "sending" : "mock");

        String runId = "run_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        Map<String, Object> pending = new LinkedHashMap<>();
        pending.put("sessionId", sessionId);
        pending.put("runId", runId);
        pending.put("status", "started");
        pending.put("answer", "");
        pending.put("createTime", new Date());
        pending.put("message", message);
        pendingRuns.put(sessionId + ":" + runId, pending);

        final String finalSessionId = sessionId;
        final String finalRunId = runId;
        final String finalMessage = message;
        final String openclawSessionKey = String.valueOf(session.get("openclawSessionKey"));
        new Thread(() -> {
            try {
                Map<String, Object> sendResult = gatewayClient.sendMessage(openclawSessionKey, finalMessage);
                String answer = String.valueOf(sendResult.getOrDefault("answer", ""));
                messageStore.computeIfAbsent(finalSessionId, key -> new ArrayList<>()).add(buildMessage("assistant", answer));
                Map<String, Object> sessionRow = sessionStore.get(finalSessionId);
                if (sessionRow != null) {
                    sessionRow.put("status", Boolean.TRUE.equals(sendResult.get("ok")) ? "gateway-connected" : (gatewayClient.enabled() ? "gateway-probe" : "mock"));
                    sessionRow.put("lastMessage", hasText(answer) ? answer : finalMessage);
                    sessionRow.put("updateTime", new Date());
                }
                pending.put("status", Boolean.TRUE.equals(sendResult.get("ok")) ? "completed" : "failed");
                pending.put("answer", answer);
                pending.put("gateway", sendResult);
                pending.put("responseTime", new Date());
            } catch (Exception ex) {
                pending.put("status", "failed");
                pending.put("error", ex.getClass().getSimpleName());
                pending.put("message", ex.getMessage());
                pending.put("responseTime", new Date());
            }
        }, "aidevops-ai-chat-" + finalRunId).start();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sessionId", sessionId);
        data.put("runId", runId);
        data.put("accepted", true);
        data.put("status", "started");
        data.put("responseTime", new Date());
        return data;
    }

    @Override
    public Map<String, Object> getSendResult(String sessionId, String runId) {
        initDefaultSession();
        String key = sessionId + ":" + runId;
        Map<String, Object> data = pendingRuns.getOrDefault(key, new LinkedHashMap<>());
        if (data.isEmpty()) {
            data.put("sessionId", sessionId);
            data.put("runId", runId);
            data.put("status", "not_found");
        }
        return data;
    }

    @Override
    public Map<String, Object> gatewayDiagnostics() {
        return gatewayClient.diagnostics();
    }

    @Override
    public Map<String, Object> probeGateway() {
        Map<String, Object> probe = gatewayClient.probeChallenge();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("probe", probe);
        result.put("mode", gatewayClient.enabled() ? "gateway-probe" : "mock-fallback");
        result.put("message", probe.get("message"));
        return result;
    }

    @Override
    public Map<String, Object> connectDraft() {
        return gatewayClient.buildConnectDraft();
    }

    @Override
    public Map<String, Object> connectTest() {
        return gatewayClient.testConnect();
    }

    @Override
    public Map<String, Object> renameSession(String sessionId, Map<String, Object> req) {
        Map<String, Object> session = sessionStore.get(sessionId);
        String title = valueOrDefault(req, "title", "未命名会话");
        if (session != null) {
            session.put("title", title);
            session.put("updateTime", new Date());
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sessionId", sessionId);
        data.put("title", title);
        return data;
    }

    @Override
    public String deleteSession(String sessionId) {
        if (!"chat_demo_main".equals(sessionId)) {
            sessionStore.remove(sessionId);
            messageStore.remove(sessionId);
        }
        return "删除成功: " + sessionId;
    }

    private Map<String, Object> buildMessage(String role, String content) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("messageId", role + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        message.put("role", role);
        message.put("content", content);
        message.put("messageTime", new Date());
        return message;
    }

    private String valueOrDefault(Map<String, Object> req, String key, String defaultValue) {
        if (req == null || req.get(key) == null) {
            return defaultValue;
        }
        String value = String.valueOf(req.get(key));
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
