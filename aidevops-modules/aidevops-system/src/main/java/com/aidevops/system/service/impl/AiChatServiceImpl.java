package com.aidevops.system.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import com.aidevops.system.config.AiChatProperties;
import com.aidevops.system.service.IAiChatService;

@Service
public class AiChatServiceImpl implements IAiChatService {

    private final AiChatProperties properties;
    private final OpenClawGatewayClient gatewayClient;
    private final Map<String, Map<String, Object>> sessionStore = new ConcurrentHashMap<>();

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
            row.put("lastMessage", "当前为原生聊天页第一版骨架，下一步接入 OpenClaw。 ");
            row.put("updateTime", new Date());
            row.put("openclawSessionKey", "main");
            return row;
        });
    }

    @Override
    public Map<String, Object> createSession(Map<String, Object> req) {
        String sessionId = "chat_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sessionId", sessionId);
        data.put("title", req != null && req.get("title") != null ? req.get("title") : "新会话");
        data.put("scene", req != null && req.get("scene") != null ? req.get("scene") : properties.getDefaultScene());
        data.put("createTime", new Date());
        data.put("status", gatewayClient.enabled() ? "gateway-ready" : "mock");
        data.put("lastMessage", gatewayClient.enabled() ? "已创建会话，待接入真实 Gateway 消息流。" : "当前为第一版接口骨架，后续接入 OpenClaw Gateway WebSocket。 ");
        data.put("openclawSessionKey", sessionId);
        sessionStore.put(sessionId, data);
        return data;
    }

    @Override
    public List<Map<String, Object>> listSessions() {
        initDefaultSession();
        return new ArrayList<>(sessionStore.values());
    }

    @Override
    public Map<String, Object> getHistory(String sessionId) {
        Map<String, Object> data = new LinkedHashMap<>();
        Map<String, Object> session = sessionStore.getOrDefault(sessionId, sessionStore.get("chat_demo_main"));
        data.put("sessionId", sessionId);
        data.put("title", session.get("title"));
        data.put("diagnostics", gatewayClient.diagnostics());
        List<Map<String, Object>> messages = new ArrayList<>();

        Map<String, Object> m1 = new LinkedHashMap<>();
        m1.put("messageId", "m_user_1");
        m1.put("role", "user");
        m1.put("content", "你好，先帮我看看这个原生聊天页结构是否正常。");
        m1.put("messageTime", new Date());
        messages.add(m1);

        Map<String, Object> m2 = new LinkedHashMap<>();
        m2.put("messageId", "m_ai_1");
        m2.put("role", "assistant");
        m2.put("content", gatewayClient.enabled()
            ? "当前 Gateway 对接配置已准备，下一步补齐 WebSocket connect.challenge/connect/chat.send/chat.history 即可切真实会话。"
            : "当前为 AI 对话页第一版骨架，后端接口已打通占位结构，后续将接入 OpenClaw Gateway 实现真实会话。"
        );
        m2.put("messageTime", new Date());
        messages.add(m2);

        data.put("messages", messages);
        data.put("status", gatewayClient.enabled() ? "gateway-ready" : "mock");
        return data;
    }

    @Override
    public Map<String, Object> sendMessage(Map<String, Object> req) {
        Map<String, Object> data = new LinkedHashMap<>();
        String sessionId = String.valueOf(req.get("sessionId"));
        String message = String.valueOf(req.get("message"));
        data.put("sessionId", sessionId);
        data.put("requestId", "req_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        data.put("answer", gatewayClient.enabled()
            ? "当前已进入 Gateway-ready 阶段：收到消息『" + message + "』。下一步将把这里切到真实 OpenClaw WebSocket 收发。"
            : "已收到消息：" + message + "。当前后端仍是第一版 mock，下一步接 OpenClaw Gateway WebSocket。"
        );
        data.put("responseTime", new Date());
        data.put("status", gatewayClient.enabled() ? "gateway-ready" : "mock");
        Map<String, Object> session = sessionStore.get(sessionId);
        if (session != null) {
            session.put("lastMessage", message);
            session.put("updateTime", new Date());
        }
        return data;
    }

    @Override
    public Map<String, Object> renameSession(String sessionId, Map<String, Object> req) {
        Map<String, Object> session = sessionStore.get(sessionId);
        if (session != null) {
            session.put("title", req.get("title"));
            session.put("updateTime", new Date());
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sessionId", sessionId);
        data.put("title", req.get("title"));
        return data;
    }

    @Override
    public String deleteSession(String sessionId) {
        sessionStore.remove(sessionId);
        return "删除成功: " + sessionId;
    }
}
