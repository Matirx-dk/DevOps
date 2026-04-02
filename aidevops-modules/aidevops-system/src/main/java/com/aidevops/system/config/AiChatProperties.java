package com.aidevops.system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "aidevops.ai.chat")
public class AiChatProperties {
    /** 是否启用 OpenClaw 对接 */
    private boolean enabled = false;
    /** OpenClaw Gateway Web 地址（预留） */
    private String gatewayUrl = "https://devops.zoudekang.cloud/openclaw/";
    /** OpenClaw Gateway WS 地址 */
    private String gatewayWsUrl = "ws://127.0.0.1:18789";
    /** token（后续真实 connect 时使用） */
    private String token;
    /** 默认场景 */
    private String defaultScene = "ops";
    /** WS 探测超时时间 */
    private int probeTimeoutMs = 5000;
    /** 是否启用实验型 Ed25519 签名器 */
    private boolean experimentalSignerEnabled = false;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getGatewayUrl() { return gatewayUrl; }
    public void setGatewayUrl(String gatewayUrl) { this.gatewayUrl = gatewayUrl; }
    public String getGatewayWsUrl() { return gatewayWsUrl; }
    public void setGatewayWsUrl(String gatewayWsUrl) { this.gatewayWsUrl = gatewayWsUrl; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getDefaultScene() { return defaultScene; }
    public void setDefaultScene(String defaultScene) { this.defaultScene = defaultScene; }
    public int getProbeTimeoutMs() { return probeTimeoutMs; }
    public void setProbeTimeoutMs(int probeTimeoutMs) { this.probeTimeoutMs = probeTimeoutMs; }
    public boolean isExperimentalSignerEnabled() { return experimentalSignerEnabled; }
    public void setExperimentalSignerEnabled(boolean experimentalSignerEnabled) { this.experimentalSignerEnabled = experimentalSignerEnabled; }
}
