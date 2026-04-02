package com.aidevops.system.service.impl;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.aidevops.system.config.AiChatProperties;

/**
 * OpenClaw Gateway 对接预留客户端。
 *
 * 说明：
 * - 当前先把系统侧结构搭好，后续再补全 connect.challenge / connect / chat.history / chat.send 的 ws 实现。
 * - 当前版本仍回退到本地 mock 流程，避免影响 test 分支现有可用性。
 */
@Component
public class OpenClawGatewayClient {

    private final AiChatProperties properties;

    public OpenClawGatewayClient(AiChatProperties properties) {
        this.properties = properties;
    }

    public boolean enabled() {
        return properties.isEnabled() && properties.getToken() != null && !properties.getToken().trim().isEmpty();
    }

    public Map<String, Object> diagnostics() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("enabled", enabled());
        data.put("gatewayUrl", properties.getGatewayUrl());
        data.put("gatewayWsUrl", properties.getGatewayWsUrl());
        data.put("mode", enabled() ? "gateway-ready" : "mock-fallback");
        data.put("message", enabled()
            ? "OpenClaw Gateway 配置已准备，后续补全 ws 协议实现后可切真实会话。"
            : "当前未启用真实 Gateway 对接，系统继续使用 mock 会话回退。"
        );
        return data;
    }
}
