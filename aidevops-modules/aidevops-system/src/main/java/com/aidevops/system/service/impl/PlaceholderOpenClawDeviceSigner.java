package com.aidevops.system.service.impl;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OpenClaw 设备签名占位实现。
 *
 * 当前目的：
 * - 先把对接结构打通，避免签名逻辑未来落地时还要大改 service/client 层。
 * - 等拿到 OpenClaw 真实 device auth 实现细节后，只需要替换这里。
 */
@Component
public class PlaceholderOpenClawDeviceSigner implements OpenClawDeviceSigner {

    @Override
    public Map<String, Object> sign(Map<String, Object> signaturePayload) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ready", false);
        result.put("algorithm", "PENDING_DEVICE_SIGNATURE_ALGORITHM");
        result.put("publicKeyFormat", "PENDING_PUBLIC_KEY_FORMAT");
        result.put("publicKey", "PENDING_DEVICE_PUBLIC_KEY");
        result.put("signature", "PENDING_DEVICE_SIGNATURE");
        result.put("payload", signaturePayload);
        result.put("notes", Arrays.asList(
            "当前为占位签名器，尚未接入真实 OpenClaw device auth 算法",
            "后续拿到算法后，只需要替换本类 sign() 的实现",
            "建议保留当前返回结构，避免前后端联调字段再次变动"
        ));
        return result;
    }
}
