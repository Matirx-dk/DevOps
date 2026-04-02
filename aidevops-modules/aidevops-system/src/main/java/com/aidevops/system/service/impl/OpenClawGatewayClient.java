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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class OpenClawGatewayClient {

    private static final int PROTOCOL_VERSION = 3;

    private final AiChatProperties properties;
    private final OpenClawDeviceSigner deviceSigner;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentHashMap<String, GatewayConnection> connections = new ConcurrentHashMap<>();

    public OpenClawGatewayClient(AiChatProperties properties, OpenClawDeviceSigner deviceSigner) {
        this.properties = properties;
        this.deviceSigner = deviceSigner;
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
        data.put("experimentalSignerEnabled", properties.isExperimentalSignerEnabled());
        data.put("connectionPoolSize", connections.size());
        data.put("probe", probeChallenge());
        data.put("connectDraft", buildConnectDraft());
        data.put("message", enabled()
            ? "已启用 Gateway 探测模式：当前可验证 challenge，并测试真实 connect。"
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
                result.put("message", "connect.challenge 已收到，可继续测试真实 connect。");
            } catch (Exception parseEx) {
                result.put("message", "WS 已连通并收到首帧，但首帧 JSON 解析失败。");
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
        Map<String, Object> signatureResult = deviceSigner.sign(castMap(signatureDraft.get("payload")));
        Map<String, Object> device = castMap(params.get("device"));
        if (signatureResult.get("suggestedDeviceId") != null) {
            device.put("id", signatureResult.get("suggestedDeviceId"));
        }
        device.put("publicKey", signatureResult.get("publicKey"));
        device.put("signature", signatureResult.get("signature"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ready", enabled());
        result.put("challengeOk", Boolean.TRUE.equals(challenge.get("ok")));
        result.put("challengeStage", challenge.get("stage"));
        result.put("signatureReady", Boolean.TRUE.equals(signatureResult.get("ready")));
        result.put("signatureDraft", signatureDraft);
        result.put("signatureResult", signatureResult);
        result.put("request", request);
        result.put("challenge", challenge);
        result.put("message", "connect 请求草稿已生成。");
        return result;
    }

    public Map<String, Object> testConnect() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("wsUrl", properties.getGatewayWsUrl());
        result.put("enabled", enabled());

        if (!enabled()) {
            result.put("ok", false);
            result.put("stage", "disabled");
            result.put("message", "未启用 Gateway，对 connect-test 直接跳过。");
            return result;
        }

        try {
            Map<String, Object> exchange = sendConnectAndReceive(properties.getGatewayWsUrl(), properties.getProbeTimeoutMs());
            Map<String, Object> request = castMap(exchange.get("request"));
            result.put("challenge", exchange.get("challenge"));
            result.put("request", request);
            result.put("requestJson", exchange.get("requestJson"));
            result.put("signatureDraft", exchange.get("signatureDraft"));
            result.put("signatureResult", exchange.get("signatureResult"));

            String responseFrame = String.valueOf(exchange.get("responseFrame"));
            result.put("ok", true);
            result.put("stage", "connect-response-received");
            result.put("responseFrame", responseFrame);
            try {
                Map<String, Object> response = objectMapper.readValue(responseFrame, new TypeReference<Map<String, Object>>() {});
                result.put("response", response);
                result.put("summary", summarizeConnectResponse(response));
                result.put("message", Boolean.TRUE.equals(response.get("ok"))
                    ? "connect 测试成功，已收到 hello-ok / success response。"
                    : "connect 已返回 response，但结果不是 ok，已提取 error/details 摘要。"
                );
            } catch (Exception ex) {
                result.put("message", "connect 已返回 response frame，但 JSON 解析失败。");
                result.put("parseError", ex.getMessage());
            }
            return result;
        } catch (Exception ex) {
            result.put("ok", false);
            result.put("stage", "connect-test-failed");
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
        result.put("connectDraft", buildConnectDraft());
        result.put("answer", enabled()
            ? "已收到消息『" + message + "』。当前已补到 connect-test 阶段，下一步是根据 Gateway 返回码继续收敛 device auth。"
            : "已收到消息：" + message + "。当前仍处于 mock 回退模式，尚未启用真实 OpenClaw Gateway。"
        );
        return result;
    }

    public Map<String, Object> sendMessage(String sessionKey, String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionKey", sessionKey);
        result.put("message", message);
        result.put("enabled", enabled());

        if (!enabled()) {
            result.put("ok", false);
            result.put("stage", "disabled");
            result.put("answer", "当前未启用真实 OpenClaw Gateway，仍处于 mock 回退模式。" );
            return result;
        }

        try {
            String effectiveSessionKey = hasText(sessionKey) ? sessionKey : "main";
            GatewayConnection connection = getOrCreateConnection(effectiveSessionKey);
            connection.touch();
            Map<String, Object> exchange = sendChatAndReceive(properties.getGatewayWsUrl(), properties.getProbeTimeoutMs(), effectiveSessionKey, message);
            connection.connected = Boolean.TRUE.equals(exchange.get("ok"));
            result.put("connectionPoolSize", connections.size());
            result.putAll(exchange);
            result.put("ok", Boolean.TRUE.equals(exchange.get("ok")));
            if (Boolean.TRUE.equals(exchange.get("ok"))) {
                result.put("answer", String.valueOf(exchange.getOrDefault("finalText", "")));
            }
            return result;
        } catch (Exception ex) {
            result.put("ok", false);
            result.put("stage", "chat-send-failed");
            result.put("error", ex.getClass().getSimpleName());
            result.put("message", ex.getMessage());
            return result;
        }
    }

    private Map<String, Object> buildConnectRequest(String nonce) {
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
        auth.put("token", properties.getToken());
        params.put("auth", auth);
        params.put("locale", "zh-CN");
        params.put("userAgent", "aidevops-system/ai-chat-gateway-bridge");
        params.put("device", buildDeviceDraft(nonce, signedAt));
        request.put("params", params);

        Map<String, Object> signatureDraft = buildSignatureDraft(request, nonce, signedAt);
        Map<String, Object> signatureResult = deviceSigner.sign(castMap(signatureDraft.get("payload")));
        Map<String, Object> device = castMap(params.get("device"));
        if (signatureResult.get("suggestedDeviceId") != null) {
            device.put("id", signatureResult.get("suggestedDeviceId"));
        }
        device.put("publicKey", signatureResult.get("publicKey"));
        device.put("signature", signatureResult.get("signature"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("request", request);
        result.put("signatureDraft", signatureDraft);
        result.put("signatureResult", signatureResult);
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
        payload.put("clientMode", client.get("mode"));
        payload.put("platform", client.get("platform"));
        payload.put("deviceFamily", client.get("deviceFamily"));
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
        client.put("id", "gateway-client");
        client.put("version", "0.1.0");
        client.put("platform", "linux");
        client.put("deviceFamily", "server");
        client.put("mode", "backend");
        return client;
    }

    private Map<String, Object> buildDeviceDraft(String nonce, long signedAt) {
        Map<String, Object> device = new LinkedHashMap<>();
        device.put("id", buildDeviceId());
        device.put("publicKey", "PENDING_DEVICE_PUBLIC_KEY");
        device.put("signature", "PENDING_DEVICE_SIGNATURE");
        device.put("signedAt", signedAt);
        device.put("nonce", nonce);
        return device;
    }

    private List<String> buildScopes() {
        return Arrays.asList("operator.read", "operator.write");
    }

    private String buildDeviceId() {
        try {
            return "aidevops-" + InetAddress.getLocalHost().getHostName();
        } catch (Exception ex) {
            return "aidevops-server";
        }
    }

    private String receiveFirstFrame(String wsUrl, int timeoutMs) throws Exception {
        CompletableFuture<String> firstFrameFuture = new CompletableFuture<>();
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(timeoutMs)).build();
        WebSocket.Listener listener = new WebSocket.Listener() {
            private final StringBuilder textBuffer = new StringBuilder();
            @Override public void onOpen(WebSocket webSocket) { webSocket.request(1); }
            @Override public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                textBuffer.append(data);
                if (last) firstFrameFuture.complete(textBuffer.toString()); else webSocket.request(1);
                return CompletableFuture.completedFuture(null);
            }
            @Override public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                if (!firstFrameFuture.isDone()) firstFrameFuture.completeExceptionally(new IllegalStateException("WS closed before first frame, code=" + statusCode + ", reason=" + reason));
                return CompletableFuture.completedFuture(null);
            }
            @Override public void onError(WebSocket webSocket, Throwable error) {
                if (!firstFrameFuture.isDone()) firstFrameFuture.completeExceptionally(error);
            }
        };
        WebSocket webSocket = client.newWebSocketBuilder().connectTimeout(Duration.ofMillis(timeoutMs)).buildAsync(URI.create(wsUrl), listener).get(timeoutMs, TimeUnit.MILLISECONDS);
        try {
            return firstFrameFuture.get(timeoutMs, TimeUnit.MILLISECONDS);
        } finally {
            try { webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "probe-complete").get(2, TimeUnit.SECONDS); } catch (Exception ignore) { webSocket.abort(); }
        }
    }

    private Map<String, Object> summarizeConnectResponse(Map<String, Object> response) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("ok", response.get("ok"));
        summary.put("type", response.get("type"));
        summary.put("id", response.get("id"));

        Map<String, Object> payload = castMap(response.get("payload"));
        Map<String, Object> error = castMap(response.get("error"));
        Map<String, Object> details = castMap(error.get("details"));

        if (!payload.isEmpty()) {
            summary.put("payloadType", payload.get("type"));
            summary.put("protocol", payload.get("protocol"));
            summary.put("policy", payload.get("policy"));
            summary.put("auth", payload.get("auth"));
        }
        if (!error.isEmpty()) {
            summary.put("errorMessage", error.get("message"));
            summary.put("errorCode", details.get("code"));
            summary.put("errorReason", details.get("reason"));
            summary.put("recommendedNextStep", details.get("recommendedNextStep"));
            summary.put("canRetryWithDeviceToken", details.get("canRetryWithDeviceToken"));
            summary.put("details", details);
            summary.put("diagnosis", diagnoseConnectError(details));
        }
        return summary;
    }

    private String diagnoseConnectError(Map<String, Object> details) {
        String code = String.valueOf(details.get("code"));
        if (code == null) {
            return "未返回标准 error.details.code，需直接查看原始 response。";
        }
        switch (code) {
            case "DEVICE_AUTH_NONCE_REQUIRED":
                return "缺少 device.nonce，说明 connect 请求未正确带上 challenge nonce。";
            case "DEVICE_AUTH_NONCE_MISMATCH":
                return "device.nonce 与服务端 challenge 不匹配，需要检查 connect-test 中的 nonce 回填。";
            case "DEVICE_AUTH_SIGNATURE_INVALID":
                return "签名原文、签名算法或签名字节编码与 OpenClaw 不一致。优先检查 payload 结构与 Base64 编码。";
            case "DEVICE_AUTH_SIGNATURE_EXPIRED":
                return "signedAt 超出允许时间窗，需要检查服务器时间和签名生成时间。";
            case "DEVICE_AUTH_DEVICE_ID_MISMATCH":
                return "device.id 与 publicKey 指纹不一致，说明 OpenClaw 可能要求 deviceId 来源于密钥指纹。";
            case "DEVICE_AUTH_PUBLIC_KEY_INVALID":
                return "publicKey 编码格式不对。当前实验 signer 使用的是 Base64(SPKI DER)，可能需要 raw key 或 JWK 对应格式。";
            case "AUTH_TOKEN_MISMATCH":
                return "Gateway token 不匹配，需要检查 aidevops.ai.chat.token 是否正确。";
            default:
                return "已拿到标准错误码，可继续按 error.details 进行定向修正。";
        }
    }

    private Map<String, Object> sendConnectAndReceive(String wsUrl, int timeoutMs) throws Exception {
        CompletableFuture<String> challengeFuture = new CompletableFuture<>();
        CompletableFuture<String> responseFuture = new CompletableFuture<>();
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(timeoutMs)).build();

        WebSocket.Listener listener = new WebSocket.Listener() {
            private final StringBuilder textBuffer = new StringBuilder();
            @Override public void onOpen(WebSocket webSocket) { webSocket.request(1); }
            @Override public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                textBuffer.append(data);
                if (last) {
                    String frame = textBuffer.toString();
                    textBuffer.setLength(0);
                    if (!challengeFuture.isDone()) challengeFuture.complete(frame); else if (!responseFuture.isDone()) responseFuture.complete(frame);
                }
                webSocket.request(1);
                return CompletableFuture.completedFuture(null);
            }
            @Override public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                if (!challengeFuture.isDone()) challengeFuture.completeExceptionally(new IllegalStateException("WS closed before challenge, code=" + statusCode + ", reason=" + reason));
                if (!responseFuture.isDone()) responseFuture.completeExceptionally(new IllegalStateException("WS closed before response, code=" + statusCode + ", reason=" + reason));
                return CompletableFuture.completedFuture(null);
            }
            @Override public void onError(WebSocket webSocket, Throwable error) {
                if (!challengeFuture.isDone()) challengeFuture.completeExceptionally(error);
                if (!responseFuture.isDone()) responseFuture.completeExceptionally(error);
            }
        };

        WebSocket webSocket = client.newWebSocketBuilder().connectTimeout(Duration.ofMillis(timeoutMs)).buildAsync(URI.create(wsUrl), listener).get(timeoutMs, TimeUnit.MILLISECONDS);
        try {
            String challengeFrame = challengeFuture.get(timeoutMs, TimeUnit.MILLISECONDS);
            Map<String, Object> challenge = objectMapper.readValue(challengeFrame, new TypeReference<Map<String, Object>>() {});
            Map<String, Object> challengePayload = castMap(challenge.get("payload"));
            String nonce = String.valueOf(challengePayload.get("nonce"));

            Map<String, Object> requestData = buildConnectRequest(nonce);
            Map<String, Object> request = castMap(requestData.get("request"));
            String requestJson = objectMapper.writeValueAsString(request);

            webSocket.sendText(requestJson, true).get(timeoutMs, TimeUnit.MILLISECONDS);
            String responseFrame = responseFuture.get(timeoutMs, TimeUnit.MILLISECONDS);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("challenge", challenge);
            result.put("request", request);
            result.put("requestJson", requestJson);
            result.put("responseFrame", responseFrame);
            result.put("signatureDraft", requestData.get("signatureDraft"));
            result.put("signatureResult", requestData.get("signatureResult"));
            return result;
        } finally {
            try { webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "connect-test-complete").get(2, TimeUnit.SECONDS); } catch (Exception ignore) { webSocket.abort(); }
        }
    }

    private Map<String, Object> sendChatAndReceive(String wsUrl, int timeoutMs, String sessionKey, String message) throws Exception {
        CompletableFuture<String> challengeFuture = new CompletableFuture<>();
        CompletableFuture<String> connectFuture = new CompletableFuture<>();
        CompletableFuture<String> chatAckFuture = new CompletableFuture<>();
        CompletableFuture<String> chatFinalFuture = new CompletableFuture<>();
        CompletableFuture<String> historyFuture = new CompletableFuture<>();
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(timeoutMs)).build();
        final String[] runIdHolder = new String[1];
        final StringBuilder latestDelta = new StringBuilder();

        WebSocket.Listener listener = new WebSocket.Listener() {
            private final StringBuilder textBuffer = new StringBuilder();
            @Override public void onOpen(WebSocket webSocket) { webSocket.request(1); }
            @Override public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                textBuffer.append(data);
                if (last) {
                    String frame = textBuffer.toString();
                    textBuffer.setLength(0);
                    try {
                        Map<String, Object> parsed = objectMapper.readValue(frame, new TypeReference<Map<String, Object>>() {});
                        String type = String.valueOf(parsed.get("type"));
                        if (!challengeFuture.isDone()) {
                            challengeFuture.complete(frame);
                        } else if (!connectFuture.isDone() && "res".equals(type)) {
                            connectFuture.complete(frame);
                        } else if (!chatAckFuture.isDone() && "res".equals(type)) {
                            chatAckFuture.complete(frame);
                            Map<String, Object> payload = castMap(parsed.get("payload"));
                            Object runId = payload.get("runId");
                            if (runId != null) runIdHolder[0] = String.valueOf(runId);
                        } else if (!historyFuture.isDone() && "res".equals(type) && String.valueOf(parsed.get("id")).startsWith("hist_")) {
                            historyFuture.complete(frame);
                        } else if ("event".equals(type) && "chat".equals(String.valueOf(parsed.get("event")))) {
                            Map<String, Object> payload = castMap(parsed.get("payload"));
                            String state = String.valueOf(payload.get("state"));
                            if (runIdHolder[0] == null || runIdHolder[0].equals(String.valueOf(payload.get("runId")))) {
                                if ("delta".equals(state)) {
                                    Map<String, Object> msg = castMap(payload.get("message"));
                                    Object text = msg.get("text");
                                    if (text != null) {
                                        latestDelta.setLength(0);
                                        latestDelta.append(String.valueOf(text));
                                    }
                                } else if ("final".equals(state) || "error".equals(state) || "aborted".equals(state)) {
                                    chatFinalFuture.complete(frame);
                                }
                            }
                        }
                    } catch (Exception ignore) {
                        if (!challengeFuture.isDone()) challengeFuture.complete(frame);
                    }
                }
                webSocket.request(1);
                return CompletableFuture.completedFuture(null);
            }
            @Override public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                if (!challengeFuture.isDone()) challengeFuture.completeExceptionally(new IllegalStateException("WS closed before challenge, code=" + statusCode + ", reason=" + reason));
                if (!connectFuture.isDone()) connectFuture.completeExceptionally(new IllegalStateException("WS closed before connect response, code=" + statusCode + ", reason=" + reason));
                if (!chatAckFuture.isDone()) chatAckFuture.completeExceptionally(new IllegalStateException("WS closed before chat ack, code=" + statusCode + ", reason=" + reason));
                if (!chatFinalFuture.isDone()) chatFinalFuture.completeExceptionally(new IllegalStateException("WS closed before chat final, code=" + statusCode + ", reason=" + reason));
                return CompletableFuture.completedFuture(null);
            }
            @Override public void onError(WebSocket webSocket, Throwable error) {
                if (!challengeFuture.isDone()) challengeFuture.completeExceptionally(error);
                if (!connectFuture.isDone()) connectFuture.completeExceptionally(error);
                if (!chatAckFuture.isDone()) chatAckFuture.completeExceptionally(error);
                if (!chatFinalFuture.isDone()) chatFinalFuture.completeExceptionally(error);
            }
        };

        WebSocket webSocket = client.newWebSocketBuilder().connectTimeout(Duration.ofMillis(timeoutMs)).buildAsync(URI.create(wsUrl), listener).get(timeoutMs, TimeUnit.MILLISECONDS);
        try {
            String challengeFrame = challengeFuture.get(timeoutMs, TimeUnit.MILLISECONDS);
            Map<String, Object> challenge = objectMapper.readValue(challengeFrame, new TypeReference<Map<String, Object>>() {});
            Map<String, Object> challengePayload = castMap(challenge.get("payload"));
            String nonce = String.valueOf(challengePayload.get("nonce"));

            Map<String, Object> connectData = buildConnectRequest(nonce);
            Map<String, Object> connectRequest = castMap(connectData.get("request"));
            String connectJson = objectMapper.writeValueAsString(connectRequest);
            webSocket.sendText(connectJson, true).get(timeoutMs, TimeUnit.MILLISECONDS);

            String connectResponseFrame = connectFuture.get(timeoutMs, TimeUnit.MILLISECONDS);
            Map<String, Object> connectResponse = objectMapper.readValue(connectResponseFrame, new TypeReference<Map<String, Object>>() {});
            if (!Boolean.TRUE.equals(connectResponse.get("ok"))) {
                Map<String, Object> failed = new LinkedHashMap<>();
                failed.put("ok", false);
                failed.put("stage", "connect-failed");
                failed.put("challenge", challenge);
                failed.put("connectRequest", connectRequest);
                failed.put("connectResponse", connectResponse);
                failed.put("connectResponseFrame", connectResponseFrame);
                failed.put("signatureDraft", connectData.get("signatureDraft"));
                failed.put("signatureResult", connectData.get("signatureResult"));
                return failed;
            }

            String idempotencyKey = "msg_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            Map<String, Object> chatRequest = new LinkedHashMap<>();
            chatRequest.put("type", "req");
            chatRequest.put("id", "chat_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
            chatRequest.put("method", "chat.send");
            Map<String, Object> chatParams = new LinkedHashMap<>();
            chatParams.put("sessionKey", hasText(sessionKey) ? sessionKey : "main");
            chatParams.put("message", message);
            chatParams.put("deliver", false);
            chatParams.put("idempotencyKey", idempotencyKey);
            chatRequest.put("params", chatParams);
            String chatJson = objectMapper.writeValueAsString(chatRequest);
            webSocket.sendText(chatJson, true).get(timeoutMs, TimeUnit.MILLISECONDS);

            String chatAckFrame = chatAckFuture.get(timeoutMs, TimeUnit.MILLISECONDS);
            Map<String, Object> chatAck = objectMapper.readValue(chatAckFrame, new TypeReference<Map<String, Object>>() {});
            String chatFinalFrame = chatFinalFuture.get(timeoutMs * 4L, TimeUnit.MILLISECONDS);
            Map<String, Object> chatFinal = objectMapper.readValue(chatFinalFrame, new TypeReference<Map<String, Object>>() {});

            Map<String, Object> historyRequest = new LinkedHashMap<>();
            historyRequest.put("type", "req");
            historyRequest.put("id", "hist_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
            historyRequest.put("method", "chat.history");
            Map<String, Object> historyParams = new LinkedHashMap<>();
            historyParams.put("sessionKey", hasText(sessionKey) ? sessionKey : "main");
            historyParams.put("limit", 20);
            historyRequest.put("params", historyParams);
            String historyJson = objectMapper.writeValueAsString(historyRequest);
            webSocket.sendText(historyJson, true).get(timeoutMs, TimeUnit.MILLISECONDS);
            String historyFrame = historyFuture.get(timeoutMs, TimeUnit.MILLISECONDS);
            Map<String, Object> historyResponse = objectMapper.readValue(historyFrame, new TypeReference<Map<String, Object>>() {});
            String finalText = extractLatestAssistantText(historyResponse);
            if (!hasText(finalText)) {
                Map<String, Object> chatPayload = castMap(chatFinal.get("payload"));
                Map<String, Object> finalMessage = castMap(chatPayload.get("message"));
                finalText = finalMessage.get("text") == null ? latestDelta.toString() : String.valueOf(finalMessage.get("text"));
            }

            Map<String, Object> ok = new LinkedHashMap<>();
            ok.put("ok", true);
            ok.put("stage", "chat-final-received");
            ok.put("challenge", challenge);
            ok.put("connectRequest", connectRequest);
            ok.put("connectResponse", connectResponse);
            ok.put("chatRequest", chatRequest);
            ok.put("chatAck", chatAck);
            ok.put("chatFinal", chatFinal);
            ok.put("historyRequest", historyRequest);
            ok.put("historyResponse", historyResponse);
            ok.put("runId", runIdHolder[0]);
            ok.put("finalText", finalText == null ? "" : finalText);
            ok.put("signatureDraft", connectData.get("signatureDraft"));
            ok.put("signatureResult", connectData.get("signatureResult"));
            return ok;
        } finally {
            try { webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "chat-send-complete").get(2, TimeUnit.SECONDS); } catch (Exception ignore) { webSocket.abort(); }
        }
    }

    private String extractLatestAssistantText(Map<String, Object> historyResponse) {
        Map<String, Object> payload = castMap(historyResponse.get("payload"));
        Object messagesObj = payload.get("messages");
        if (!(messagesObj instanceof List<?> messages)) {
            return "";
        }
        for (int i = messages.size() - 1; i >= 0; i--) {
            Object item = messages.get(i);
            if (!(item instanceof Map<?, ?> raw)) {
                continue;
            }
            Map<String, Object> message = castMap(raw);
            String role = String.valueOf(message.get("role"));
            if (!"assistant".equalsIgnoreCase(role)) {
                continue;
            }
            Object text = message.get("text");
            if (text != null && hasText(String.valueOf(text))) {
                return String.valueOf(text);
            }
            Object contentObj = message.get("content");
            if (contentObj instanceof List<?> contentList) {
                StringBuilder sb = new StringBuilder();
                for (Object block : contentList) {
                    if (!(block instanceof Map<?, ?> rawBlock)) {
                        continue;
                    }
                    Map<String, Object> content = castMap(rawBlock);
                    if ("text".equals(String.valueOf(content.get("type"))) && content.get("text") != null) {
                        if (sb.length() > 0) {
                            sb.append("\n");
                        }
                        sb.append(String.valueOf(content.get("text")));
                    }
                }
                if (hasText(sb.toString())) {
                    return sb.toString();
                }
            }
        }
        return "";
    }

    private GatewayConnection getOrCreateConnection(String sessionKey) {
        return connections.computeIfAbsent(sessionKey, GatewayConnection::new);
    }

    private final class GatewayConnection {
        private final String sessionKey;
        private volatile long lastUsedAt = System.currentTimeMillis();
        private volatile boolean connected;

        private GatewayConnection(String sessionKey) {
            this.sessionKey = sessionKey;
        }

        private void touch() {
            this.lastUsedAt = System.currentTimeMillis();
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
