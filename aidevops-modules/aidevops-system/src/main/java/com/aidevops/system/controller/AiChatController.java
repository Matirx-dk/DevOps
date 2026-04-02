package com.aidevops.system.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.aidevops.common.core.web.controller.BaseController;
import com.aidevops.common.core.web.domain.AjaxResult;
import com.aidevops.common.core.web.page.TableDataInfo;
import com.aidevops.system.service.IAiChatService;

@RestController
@RequestMapping("/ai/chat")
public class AiChatController extends BaseController {

    @Autowired
    private IAiChatService aiChatService;

    @PostMapping("/session")
    public AjaxResult createSession(@RequestBody(required = false) Map<String, Object> req) {
        return success(aiChatService.createSession(req));
    }

    @GetMapping("/session/list")
    public TableDataInfo list() {
        return getDataTable(aiChatService.listSessions());
    }

    @GetMapping("/history/{sessionId}")
    public AjaxResult history(@PathVariable String sessionId) {
        return success(aiChatService.getHistory(sessionId));
    }

    @PostMapping("/send")
    public AjaxResult send(@RequestBody Map<String, Object> req) {
        return success(aiChatService.sendMessage(req));
    }

    @GetMapping("/send/result/{sessionId}/{runId}")
    public AjaxResult sendResult(@PathVariable String sessionId, @PathVariable String runId) {
        return success(aiChatService.getSendResult(sessionId, runId));
    }

    @GetMapping("/gateway/diagnostics")
    public AjaxResult gatewayDiagnostics() {
        return success(aiChatService.gatewayDiagnostics());
    }

    @PostMapping("/gateway/probe")
    public AjaxResult probeGateway() {
        return success(aiChatService.probeGateway());
    }

    @GetMapping("/gateway/connect-draft")
    public AjaxResult connectDraft() {
        return success(aiChatService.connectDraft());
    }

    @PostMapping("/gateway/connect-test")
    public AjaxResult connectTest() {
        return success(aiChatService.connectTest());
    }

    @PutMapping("/session/{sessionId}")
    public AjaxResult rename(@PathVariable String sessionId, @RequestBody Map<String, Object> req) {
        return success(aiChatService.renameSession(sessionId, req));
    }

    @DeleteMapping("/session/{sessionId}")
    public AjaxResult delete(@PathVariable String sessionId) {
        return success(aiChatService.deleteSession(sessionId));
    }
}
