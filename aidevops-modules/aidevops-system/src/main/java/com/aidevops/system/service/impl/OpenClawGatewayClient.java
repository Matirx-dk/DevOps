package com.aidevops.system.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.aidevops.system.config.AiChatProperties;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

/**
 * OpenClaw Gateway 对接客户端（当前阶段：先做真实 challenge 探测，不直接硬上完整 connect）。
 *
 * 说明：
 * - 按 OpenClaw Gateway 协议，WS 连接建立后服务端会先推送 connect.challenge。
 * - 完整 connect 还需要 device 签名与鉴权，这里先把 challenge 探测能力补上，
 *   便于后续继续接真实 auth/connect/chat.send。
 */
@Component
public class OpenClawGatewayClient {

    private final AiChatProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenClawGatewayClient(AiChatProperties properties) {
        this.properties = properties;
    }

    public boolean enabled() {
        return properties.isEnabled();
    }

    public Map<String, Object> diagnostics() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("enabled", enabled());
        data.put("gatewayUrl", properties.getGatewayUrl());
        data.put("gatewayWsUrl", properties.getGatewayWsUrl());
        data.put("tokenConfigured", hasText(properties.getToken()));
        data.put("mode", enabled() ? "gateway-probe" : "mock-fallback");
        data.put("probe", probeChallenge());
        data.put("message", enabled()
            ? "已启用 Gateway 探测模式：当前先验证 WS challenge，下一步补 connect/auth/chat.send。"
            : "当前未启用真实 Gateway 对接，系统继续使用本地 mock 会话回退。"
        );
        return data;
    }

    public Map<String, Object> probeChallenge() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("requestId", "probe_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        result.put("wsUrl", properties.getGatewayWsUrl());
        result.put("timeoutMs", properties.getProbeTimeoutMs());

        if (!enabled()) {
            result.put("ok", false);
            result.put("stage", "disabled");
            result.put("message", "AI 对话 Gateway 对接未启用，未执行 WS 探测。");
            return result;
        }

        try {
            String firstFrame = receiveFirstFrame(properties.getGatewayWsUrl(), properties.getProbeTimeoutMs());
            result.put("ok", true);
            result.put("stage", "challenge-received");
            result.put("firstFrame", firstFrame);
            try {
                Map<String, Object> frame = objectMapper.readValue(firstFrame, new TypeReference<Map<String, Object>>() {});
                result.put("frameType", frame.get("type"));
                result.put("event", frame.get("event"));
                Object payload = frame.get("payload");
                result.put("payload", payload);
                if (payload instanceof Map<?, ?> payloadMap) {
                    result.put("nonce", payloadMap.get("nonce"));
                    result.put("ts", payloadMap.get("ts"));
                }
                if (!"connect.challenge".equals(String.valueOf(frame.get("event")))) {
                    result.put("message", "WS 已连通，但首帧不是预期的 connect.challenge，需继续核对网关协议或入口地址。");
                } else {
                    result.put("message", "WS 已连通，已收到 connect.challenge。下一步需要补设备签名与 connect 请求。\n");
                }
            } catch (Exception parseEx) {
                result.put("message", "WS 已连通并收到首帧，但首帧 JSON 解析失败。\n");
                result.put("parseError", parseEx.getMessage());
            }
            return result;
        } catch (Exception ex) {
            result.put("ok", false);
            result.put("stage", "connect-failed");
            result.put("error", ex.getClass().getSimpleName());
            result.put("message", ex.getMessage());
            return result;
        }
    }

    public Map<String, Object> previewSend(String sessionKey, String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionKey", sessionKey);
        result.put("sent", false);
        result.put("gatewayMode", enabled() ? "probe" : "mock");
        result.put("probe", probeChallenge());

        if (!enabled()) {
            result.put("answer", "已收到消息：" + message + "。当前仍处于 mock 回退模式，尚未启用真实 OpenClaw Gateway。");
            return result;
        }

        Object probeOk = ((Map<?, ?>) result.get("probe")).get("ok");
        if (Boolean.TRUE.equals(probeOk)) {
            result.put("answer", "已收到消息『" + message + "』。当前已验证 Gateway WS challenge 可达，但完整 connect/auth/chat.send 还未补齐，因此这次先不直发真实 OpenClaw 会话。");
        } else {
            result.put("answer", "已收到消息『" + message + "』。当前 Gateway WS challenge 仍未探测成功，本次继续走本地回退。\n");
        }
        return result;
    }

    private String receiveFirstFrame(String wsUrl, int timeoutMs) throws Exception {
        CompletableFuture<String> firstFrameFuture = new CompletableFuture<>();
        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(timeoutMs))
            .build();

        WebSocket.Listener listener = new WebSocket.Listener() {
            private final StringBuilder textBuffer = new StringBuilder();

            @Override
            public void onOpen(WebSocket webSocket) {
                webSocket.request(1);
                WebSocket.Listener.super.onOpen(webSocket);
            }

            @Override
            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                textBuffer.append(data);
                if (last) {
                    firstFrameFuture.complete(textBuffer.toString());
                } else {
                    webSocket.request(1);
                }
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                if (!firstFrameFuture.isDone()) {
                    firstFrameFuture.completeExceptionally(new IllegalStateException("WS closed before first frame, code=" + statusCode + ", reason=" + reason));
                }
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public void onError(WebSocket webSocket, Throwable error) {
                if (!firstFrameFuture.isDone()) {
                    firstFrameFuture.completeExceptionally(error);
                }
            }
        };

        WebSocket webSocket = client.newWebSocketBuilder()
            .connectTimeout(Duration.ofMillis(timeoutMs))
            .buildAsync(URI.create(wsUrl), listener)
            .get(timeoutMs, TimeUnit.MILLISECONDS);

        try {
            return firstFrameFuture.get(timeoutMs, TimeUnit.MILLISECONDS);
        } finally {
            try {
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "probe-complete").get(2, TimeUnit.SECONDS);
            } catch (Exception ignore) {
                webSocket.abort();
            }
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
