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
            row.put("lastMessage", "当前已接入会话存储与 Gateway challenge 探测能力。");
            row.put("createTime", new Date());
            row.put("updateTime", new Date());
            row.put("openclawSessionKey", "main");
            row.put("status", gatewayClient.enabled() ? "gateway-probe" : "mock");
            return row;
        });

        messageStore.computeIfAbsent("chat_demo_main", key -> {
            List<Map<String, Object>> messages = new ArrayList<>();
            messages.add(buildMessage("assistant", "你好，这里是 AI 运维对话页。当前后端已经不是纯 mock，而是补上了本地消息存储和 Gateway WS challenge 探测。"));
            messages.add(buildMessage("assistant", gatewayClient.enabled()
                ? "当前已启用 Gateway 探测模式。下一步是继续补 connect.challenge 之后的设备签名与 connect/chat.send。"
                : "当前未启用真实 Gateway 配置，所以仍会先走本地回退流程。"
            ));
            return messages;
        });
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
        data.put("openclawSessionKey", sessionId);
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
        session.put("status", gatewayClient.enabled() ? "gateway-probe" : "mock");

        Map<String, Object> preview = gatewayClient.previewSend(String.valueOf(session.get("openclawSessionKey")), message);
        String answer = String.valueOf(preview.get("answer"));
        messages.add(buildMessage("assistant", answer));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sessionId", sessionId);
        data.put("requestId", "req_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        data.put("answer", answer);
        data.put("responseTime", new Date());
        data.put("status", session.get("status"));
        data.put("probe", preview.get("probe"));
        return data;
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
}
