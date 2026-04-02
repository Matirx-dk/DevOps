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
            String canonicalPayload = buildCanonicalPayload(signaturePayload);
            byte[] payloadBytes = canonicalPayload.getBytes(StandardCharsets.UTF_8);
            byte[] publicKeyRaw = exportRawEd25519PublicKey(keyPair.getPublic());
            byte[] signatureBytes = signEd25519(keyPair.getPrivate(), payloadBytes);
            String fingerprintSha256 = sha256Hex(publicKeyRaw);
            String suggestedDeviceId = fingerprintSha256;

            result.put("ready", true);
            result.put("mode", "experimental-ed25519");
            result.put("algorithm", "Ed25519");
            result.put("publicKeyFormat", "base64url-raw-ed25519-32" );
            result.put("publicKey", base64UrlNoPadding(publicKeyRaw));
            result.put("signature", base64UrlNoPadding(signatureBytes));
            result.put("publicKeyFingerprintSha256", fingerprintSha256);
            result.put("suggestedDeviceId", suggestedDeviceId);
            result.put("deviceIdCandidates", Arrays.asList(
                suggestedDeviceId,
                "fingerprint:" + fingerprintSha256,
                "aidevops-server"
            ));
            result.put("payload", signaturePayload);
            result.put("payloadCanonical", canonicalPayload);
            result.put("notes", Arrays.asList(
                "这是实验型 Ed25519 signer，用于验证 AI 对话后端到 OpenClaw Gateway 的签名字段链路",
                "已按 OpenClaw 控制端源码切到 v2 canonical payload + raw Ed25519 publicKey + base64url(no padding) 编码",
                "如果 Gateway 仍返回 DEVICE_AUTH_SIGNATURE_INVALID，需要继续检查 canonical payload 字段顺序或 Java Ed25519 原始公钥提取方式"
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

    private byte[] exportRawEd25519PublicKey(PublicKey publicKey) {
        byte[] encoded = publicKey.getEncoded();
        if (encoded == null || encoded.length < 32) {
            throw new IllegalStateException("Ed25519 public key encoding is invalid");
        }
        return Arrays.copyOfRange(encoded, encoded.length - 32, encoded.length);
    }

    private String buildCanonicalPayload(Map<String, Object> signaturePayload) {
        String deviceId = stringValue(signaturePayload.get("deviceId"));
        String clientId = stringValue(signaturePayload.get("clientId"));
        String clientMode = stringValue(signaturePayload.get("clientMode"));
        String role = stringValue(signaturePayload.get("role"));
        String scopes = stringValue(signaturePayload.get("scopes")).replace(" ", "");
        String signedAt = stringValue(signaturePayload.get("signedAt"));
        String token = stringValue(signaturePayload.get("token"));
        String nonce = stringValue(signaturePayload.get("nonce"));
        return String.join("|", "v2", deviceId, clientId, clientMode, role, scopes, signedAt, token, nonce);
    }

    private String stringValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Iterable<?> iterable) {
            StringBuilder sb = new StringBuilder();
            for (Object item : iterable) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(stringValue(item).trim());
            }
            return sb.toString();
        }
        return String.valueOf(value);
    }

    private String base64UrlNoPadding(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
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
