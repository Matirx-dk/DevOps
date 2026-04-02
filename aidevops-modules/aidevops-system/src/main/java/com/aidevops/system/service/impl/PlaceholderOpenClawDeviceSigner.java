package com.aidevops.system.service.impl;

import com.aidevops.system.config.AiChatProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
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
            Map<String, Object> nodeResult = nodeOfficialSigner(signaturePayload);
            if (Boolean.TRUE.equals(nodeResult.get("ready"))) {
                return nodeResult;
            }
            Map<String, Object> javaResult = experimentalEd25519Sign(signaturePayload);
            if (!Boolean.TRUE.equals(javaResult.get("ready"))) {
                javaResult.put("nodeFallback", nodeResult);
            } else {
                javaResult.put("nodeFallback", nodeResult);
            }
            return javaResult;
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

    private Map<String, Object> nodeOfficialSigner(Map<String, Object> signaturePayload) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            String nodeScript = "const crypto=require('node:crypto');"
                + "const fs=require('node:fs');"
                + "const path=require('node:path');"
                + "const input=JSON.parse(fs.readFileSync(0,'utf8'));"
                + "const identityPath=process.env.AIDEVOPS_OPENCLAW_IDENTITY_PATH||'/tmp/aidevops-openclaw-device-identity.json';"
                + "const norm=v=>typeof v==='string'&&v.trim()?v.trim().replace(/[A-Z]/g,c=>String.fromCharCode(c.charCodeAt(0)+32)) : '';"
                + "const b64u=b=>Buffer.from(b).toString('base64').replace(/\\+/g,'-').replace(/\\//g,'_').replace(/=+$/g,'');"
                + "const prefix=Buffer.from('302a300506032b6570032100','hex');"
                + "const rawFromPem=pem=>{const spki=crypto.createPublicKey(pem).export({type:'spki',format:'der'});return (spki.length===prefix.length+32&&spki.subarray(0,prefix.length).equals(prefix))?spki.subarray(prefix.length):spki;};"
                + "const deriveId=pem=>crypto.createHash('sha256').update(rawFromPem(pem)).digest('hex');"
                + "const loadOrCreate=()=>{try{const raw=fs.readFileSync(identityPath,'utf8');const parsed=JSON.parse(raw);if(parsed&&parsed.publicKeyPem&&parsed.privateKeyPem){return {deviceId:parsed.deviceId||deriveId(parsed.publicKeyPem),publicKeyPem:parsed.publicKeyPem,privateKeyPem:parsed.privateKeyPem,createdAtMs:parsed.createdAtMs||Date.now(),loaded:true};}}catch{} const kp=crypto.generateKeyPairSync('ed25519'); const publicKeyPem=kp.publicKey.export({type:'spki',format:'pem'}).toString(); const privateKeyPem=kp.privateKey.export({type:'pkcs8',format:'pem'}).toString(); const deviceId=deriveId(publicKeyPem); const out={version:1,deviceId,publicKeyPem,privateKeyPem,createdAtMs:Date.now()}; fs.mkdirSync(path.dirname(identityPath),{recursive:true}); fs.writeFileSync(identityPath, JSON.stringify(out,null,2)+'\\n'); return {...out,loaded:false};};"
                + "const identity=loadOrCreate();"
                + "const raw=rawFromPem(identity.publicKeyPem);"
                + "const deviceId=identity.deviceId;"
                + "const payload=['v3',deviceId,String(input.clientId||''),String(input.clientMode||''),String(input.role||''),Array.isArray(input.scopes)?input.scopes.map(x=>String(x).trim()).join(','):String(input.scopes||'').replace(/ /g,''),String(input.signedAt||''),String(input.token||''),String(input.nonce||''),norm(String(input.platform||'')),norm(String(input.deviceFamily||''))].join('|');"
                + "const sig=crypto.sign(null,Buffer.from(payload,'utf8'),crypto.createPrivateKey(identity.privateKeyPem));"
                + "process.stdout.write(JSON.stringify({ready:true,mode:'node-official-ed25519',algorithm:'Ed25519',publicKeyFormat:'base64url-raw-ed25519-32',publicKey:b64u(raw),signature:b64u(sig),publicKeyFingerprintSha256:deviceId,suggestedDeviceId:deviceId,payloadCanonical:payload,privateKeyPem:identity.privateKeyPem,publicKeyPem:identity.publicKeyPem,identityPath,identityLoaded:identity.loaded,createdAtMs:identity.createdAtMs}));";

            Process process = new ProcessBuilder("node", "-e", nodeScript).start();
            process.getOutputStream().write(objectMapper.writeValueAsBytes(signaturePayload));
            process.getOutputStream().close();

            String stdout = readAll(process.getInputStream());
            String stderr = readAll(process.getErrorStream());
            int exit = process.waitFor();
            if (exit != 0) {
                result.put("ready", false);
                result.put("mode", "node-official-ed25519-failed");
                result.put("error", "NodeSignerExit" + exit);
                result.put("message", stderr.isBlank() ? stdout : stderr);
                return result;
            }
            Map<String, Object> parsed = objectMapper.readValue(stdout, Map.class);
            parsed.put("payload", signaturePayload);
            parsed.put("notes", Arrays.asList(
                "已走本地 Node 官方同构 signer",
                "签名路径对齐 OpenClaw Node client: crypto.sign(null, payloadUtf8, privateKeyPem)",
                "若此结果可过签，则说明 Java JCA signer 确实是根因"
            ));
            return parsed;
        } catch (Exception ex) {
            result.put("ready", false);
            result.put("mode", "node-official-ed25519-failed");
            result.put("error", ex.getClass().getSimpleName());
            result.put("message", ex.getMessage());
            return result;
        }
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
                "这是 Java 实验型 Ed25519 signer 回退路径",
                "仅在本地 Node signer 不可用或执行失败时启用",
                "若 Gateway 仍返回 DEVICE_AUTH_SIGNATURE_INVALID，优先使用 Node 官方同构 signer 结果判断"
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

    private static final byte[] ED25519_SPKI_PREFIX = new byte[] {
        0x30, 0x2a, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x70, 0x03, 0x21, 0x00
    };

    private byte[] exportRawEd25519PublicKey(PublicKey publicKey) {
        byte[] encoded = publicKey.getEncoded();
        if (encoded == null || encoded.length == 0) {
            throw new IllegalStateException("Ed25519 public key encoding is invalid");
        }
        if (encoded.length == ED25519_SPKI_PREFIX.length + 32 && startsWith(encoded, ED25519_SPKI_PREFIX)) {
            return Arrays.copyOfRange(encoded, ED25519_SPKI_PREFIX.length, encoded.length);
        }
        if (encoded.length >= 32) {
            return Arrays.copyOfRange(encoded, encoded.length - 32, encoded.length);
        }
        throw new IllegalStateException("Ed25519 public key encoding is too short");
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
        String platform = normalizeDeviceMetadataForAuth(stringValue(signaturePayload.get("platform")));
        String deviceFamily = normalizeDeviceMetadataForAuth(stringValue(signaturePayload.get("deviceFamily")));
        return String.join("|", "v3", deviceId, clientId, clientMode, role, scopes, signedAt, token, nonce, platform, deviceFamily);
    }

    private boolean startsWith(byte[] value, byte[] prefix) {
        if (value.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (value[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private String normalizeDeviceMetadataForAuth(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(trimmed.length());
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                sb.append((char) (c + 32));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
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

    private String readAll(InputStream inputStream) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int n;
        while ((n = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, n);
        }
        return outputStream.toString(StandardCharsets.UTF_8);
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
