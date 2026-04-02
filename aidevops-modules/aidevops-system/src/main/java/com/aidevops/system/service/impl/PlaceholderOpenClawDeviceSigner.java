package com.aidevops.system.service.impl;

import com.aidevops.system.config.AiChatProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OpenClaw 设备签名器。
 *
 * 默认：返回占位结果，保证现有联调结构稳定。
 * 实验模式：启用 Ed25519 真实签名，用于快速验证字段链路。
 */
@Component
public class PlaceholderOpenClawDeviceSigner implements OpenClawDeviceSigner {

    private final AiChatProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private volatile KeyPair cachedKeyPair;

    public PlaceholderOpenClawDeviceSigner(AiChatProperties properties) {
        this.properties = properties;
    }

    @Override
    public Map<String, Object> sign(Map<String, Object> signaturePayload) {
        if (properties.isExperimentalSignerEnabled()) {
            return experimentalEd25519Sign(signaturePayload);
        }
        return placeholder(signaturePayload);
    }

    private Map<String, Object> placeholder(Map<String, Object> signaturePayload) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ready", false);
        result.put("mode", "placeholder");
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

    private Map<String, Object> experimentalEd25519Sign(Map<String, Object> signaturePayload) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            KeyPair keyPair = getOrCreateKeyPair();
            String payloadJson = objectMapper.writeValueAsString(signaturePayload);
            byte[] payloadBytes = payloadJson.getBytes(StandardCharsets.UTF_8);
            byte[] publicKeyDer = exportPublicKey(keyPair.getPublic());
            byte[] signatureBytes = signEd25519(keyPair.getPrivate(), payloadBytes);
            String fingerprintSha256 = sha256Hex(publicKeyDer);
            String suggestedDeviceId = "aidevops-" + fingerprintSha256.substring(0, 16);

            result.put("ready", true);
            result.put("mode", "experimental-ed25519");
            result.put("algorithm", "Ed25519");
            result.put("publicKeyFormat", "base64-spki-der");
            result.put("publicKey", Base64.getEncoder().encodeToString(publicKeyDer));
            result.put("signature", Base64.getEncoder().encodeToString(signatureBytes));
            result.put("publicKeyFingerprintSha256", fingerprintSha256);
            result.put("suggestedDeviceId", suggestedDeviceId);
            result.put("deviceIdCandidates", Arrays.asList(
                suggestedDeviceId,
                "fingerprint:" + fingerprintSha256,
                "aidevops-server"
            ));
            result.put("payload", signaturePayload);
            result.put("payloadJson", payloadJson);
            result.put("notes", Arrays.asList(
                "这是实验型 Ed25519 signer，用于验证 AI 对话后端到 OpenClaw Gateway 的签名字段链路",
                "当前 publicKey 使用 Base64(SPKI DER) 编码，是否与 OpenClaw device auth 最终格式完全一致，仍需继续对齐",
                "如果 Gateway 返回 DEVICE_AUTH_DEVICE_ID_MISMATCH，可优先尝试 fingerprint 派生的 deviceId",
                "如果 Gateway 返回 DEVICE_AUTH_PUBLIC_KEY_INVALID，需要继续调整 publicKey 编码格式"
            ));
            return result;
        } catch (Exception ex) {
            result.put("ready", false);
            result.put("mode", "experimental-ed25519-failed");
            result.put("algorithm", "Ed25519");
            result.put("payload", signaturePayload);
            result.put("error", ex.getClass().getSimpleName());
            result.put("message", ex.getMessage());
            result.put("notes", Arrays.asList(
                "实验型 signer 执行失败，当前已自动回到未就绪状态",
                "可先保持 connect 草稿联调，不影响其他页面与接口继续使用"
            ));
            return result;
        }
    }

    private KeyPair getOrCreateKeyPair() throws Exception {
        if (cachedKeyPair != null) {
            return cachedKeyPair;
        }
        synchronized (this) {
            if (cachedKeyPair == null) {
                KeyPairGenerator generator = KeyPairGenerator.getInstance("Ed25519");
                cachedKeyPair = generator.generateKeyPair();
            }
            return cachedKeyPair;
        }
    }

    private byte[] signEd25519(PrivateKey privateKey, byte[] payloadBytes) throws Exception {
        Signature signature = Signature.getInstance("Ed25519");
        signature.initSign(privateKey);
        signature.update(payloadBytes);
        return signature.sign();
    }

    private byte[] exportPublicKey(PublicKey publicKey) {
        return publicKey.getEncoded();
    }

    private String sha256Hex(byte[] bytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashed = digest.digest(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : hashed) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
