package com.aidevops.system.service.impl;

import java.util.Map;

public interface OpenClawDeviceSigner {
    Map<String, Object> sign(Map<String, Object> signaturePayload);
}
