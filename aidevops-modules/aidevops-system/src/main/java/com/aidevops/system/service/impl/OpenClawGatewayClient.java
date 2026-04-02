package com.aidevops.system.service.impl;

import com.aidevops.system.config.AiChatProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

/**
 * OpenClaw Gateway 对接客户端（当前阶段：真实 challenge 探测 + connect 请求草稿生成）。
 */
@Component
public class OpenClawGatewayClient {

    private static final int PROTOCOL_VERSION = 3;

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
        data.put("connectDraft", buildConnectDraft());
        data.put("message", enabled()
            ? "已启用 Gateway 探测模式：当前先验证 WS challenge，并生成 connect 请求草稿。"
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

    public Map<String, Object> buildConnectDraft() {
        Map<String, Object> challenge = probeChallenge();
        String nonce = challenge.get("nonce") == null ? "PENDING_SERVER_CHALLENGE" : String.valueOf(challenge.get("nonce"));
        long signedAt = System.currentTimeMillis();

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("type", "req");
        request.put("id", "conn_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        request.put("method", "connect");

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("minProtocol", PROTOCOL_VERSION);
        params.put("maxProtocol", PROTOCOL_VERSION);
        params.put("client", buildClientInfo());
        params.put("role", "operator");
        params.put("scopes", buildScopes());
        params.put("caps", new ArrayList<>());
        params.put("commands", new ArrayList<>());
        params.put("permissions", new LinkedHashMap<>());

        Map<String, Object> auth = new LinkedHashMap<>();
        auth.put("token", hasText(properties.getToken()) ? properties.getToken() : "PENDING_GATEWAY_TOKEN");
        params.put("auth", auth);
        params.put("locale", "zh-CN");
        params.put("userAgent", "aidevops-system/ai-chat-gateway-bridge");
        params.put("device", buildDeviceDraft(nonce, signedAt));

        request.put("params", params);

        Map<String, Object> signatureDraft = buildSignatureDraft(request, nonce, signedAt);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ready", enabled());
        result.put("challengeOk", Boolean.TRUE.equals(challenge.get("ok")));
        result.put("challengeStage", challenge.get("stage"));
        result.put("signatureReady", false);
        result.put("signatureDraft", signatureDraft);
        result.put("message", Boolean.TRUE.equals(challenge.get("ok"))
            ? "connect 请求草稿已生成；待签名原文也已固定，当前还缺真实 device 签名算法。"
            : "connect 请求草稿已生成；但当前还未拿到 challenge，nonce 先用占位值。"
        );
        result.put("request", request);
        result.put("challenge", challenge);
        result.put("todo", Arrays.asList(
            "用服务端返回的 connect.challenge.nonce 替换占位 nonce",
            "按 OpenClaw device auth 规则生成 publicKey/signature",
            "将 signature/publicKey 回填到 connect 请求",
            "发送 connect 请求并接收 hello-ok",
            "在 connect 成功后再继续补 chat.send / history"
        ));
        return result;
    }

    public Map<String, Object> previewSend(String sessionKey, String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionKey", sessionKey);
        result.put("sent", false);
        result.put("gatewayMode", enabled() ? "probe" : "mock");
        result.put("probe", probeChallenge());
        result.put("connectDraft", buildConnectDraft());

        if (!enabled()) {
            result.put("answer", "已收到消息：" + message + "。当前仍处于 mock 回退模式，尚未启用真实 OpenClaw Gateway。");
            return result;
        }

        Object probeOk = ((Map<?, ?>) result.get("probe")).get("ok");
        if (Boolean.TRUE.equals(probeOk)) {
            result.put("answer", "已收到消息『" + message + "』。当前已验证 Gateway WS challenge 可达，也已生成 connect 请求草稿；下一步只差 device 签名与真实 connect/chat.send。");
        } else {
            result.put("answer", "已收到消息『" + message + "』。当前 Gateway WS challenge 仍未探测成功，本次继续走本地回退。\n");
        }
        return result;
    }

    private Map<String, Object> buildSignatureDraft(Map<String, Object> request, String nonce, long signedAt) {
        Map<String, Object> params = castMap(request.get("params"));
        Map<String, Object> client = castMap(params.get("client"));
        Map<String, Object> auth = castMap(params.get("auth"));
        Map<String, Object> device = castMap(params.get("device"));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("version", "v3");
        payload.put("deviceId", device.get("id"));
        payload.put("clientId", client.get("id"));
        payload.put("clientVersion", client.get("version"));
        payload.put("platform", client.get("platform"));
        payload.put("deviceFamily", device.get("deviceFamily"));
        payload.put("role", params.get("role"));
        payload.put("scopes", params.get("scopes"));
        payload.put("token", auth.get("token"));
        payload.put("nonce", nonce);
        payload.put("signedAt", signedAt);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("algorithm", "PENDING_DEVICE_SIGNATURE_ALGORITHM");
        result.put("publicKeyFormat", "PENDING_PUBLIC_KEY_FORMAT");
        result.put("payload", payload);
        try {
            result.put("payloadJson", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload));
        } catch (Exception ex) {
            result.put("payloadJson", String.valueOf(payload));
        }
        result.put("notes", Arrays.asList(
            "当前根据 OpenClaw protocol.md 固定了 v3 待签名字段集合",
            "真实签名算法、密钥生成方式、publicKey 编码格式仍需继续对齐 OpenClaw 实现",
            "如果服务端拒绝 v3，可回退验证 legacy v2 payload"
        ));
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        if (value instanceof Map<?, ?>) {
            return (Map<String, Object>) value;
        }
        return new LinkedHashMap<>();
    }

    private Map<String, Object> buildClientInfo() {
        Map<String, Object> client = new LinkedHashMap<>();
        client.put("id", "aidevops-ai-chat");
        client.put("version", "0.1.0");
        client.put("platform", "linux");
        client.put("mode", "operator");
        return client;
    }

    private Map<String, Object> buildDeviceDraft(String nonce, long signedAt) {
        Map<String, Object> device = new LinkedHashMap<>();
        device.put("id", buildDeviceId());
        device.put("publicKey", "PENDING_DEVICE_PUBLIC_KEY");
        device.put("signature", "PENDING_DEVICE_SIGNATURE");
        device.put("signedAt", signedAt);
        device.put("nonce", nonce);
        device.put("deviceFamily", "server");
        device.put("signatureVersion", "v3");
        return device;
    }

    private List<String> buildScopes() {
        return Arrays.asList("operator.read", "operator.write");
    }

    private String buildDeviceId() {
        try {
            String host = InetAddress.getLocalHost().getHostName();
            return "aidevops-" + host;
        } catch (Exception ex) {
            return "aidevops-server";
        }
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
