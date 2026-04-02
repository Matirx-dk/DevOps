package com.aidevops.system.service;

import java.util.List;
import java.util.Map;

public interface IAiChatService {
    Map<String, Object> createSession(Map<String, Object> req);
    List<Map<String, Object>> listSessions();
    Map<String, Object> getHistory(String sessionId);
    Map<String, Object> sendMessage(Map<String, Object> req);
    Map<String, Object> renameSession(String sessionId, Map<String, Object> req);
    String deleteSession(String sessionId);
    Map<String, Object> gatewayDiagnostics();
    Map<String, Object> probeGateway();
    Map<String, Object> connectDraft();
    Map<String, Object> connectTest();
}
