package com.aidevops.system.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

/**
 * AI对话接口（第一版骨架）
 */
@RestController
@RequestMapping("/ai/chat")
public class AiChatController extends BaseController {

    @PostMapping("/session")
    public AjaxResult createSession(@RequestBody(required = false) Map<String, Object> req) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sessionId", "chat_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        data.put("title", req != null && req.get("title") != null ? req.get("title") : "新会话");
        data.put("scene", req != null && req.get("scene") != null ? req.get("scene") : "ops");
        data.put("createTime", new Date());
        data.put("status", "mock");
        data.put("message", "当前为第一版接口骨架，后续接入 OpenClaw Gateway WebSocket。");
        return success(data);
    }

    @GetMapping("/session/list")
    public TableDataInfo list() {
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("sessionId", "chat_demo_main");
        row.put("title", "AI运维对话");
        row.put("scene", "ops");
        row.put("lastMessage", "当前为原生聊天页第一版骨架，待接入 OpenClaw。 ");
        row.put("updateTime", new Date());
        list.add(row);
        TableDataInfo rsp = new TableDataInfo();
        rsp.setRows(list);
        rsp.setTotal(list.size());
        rsp.setCode(200);
        rsp.setMsg("查询成功");
        return rsp;
    }

    @GetMapping("/history/{sessionId}")
    public AjaxResult history(@PathVariable String sessionId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sessionId", sessionId);
        data.put("title", "AI运维对话");
        List<Map<String, Object>> messages = new ArrayList<>();

        Map<String, Object> m1 = new LinkedHashMap<>();
        m1.put("messageId", "m_user_1");
        m1.put("role", "user");
        m1.put("content", "你好，先帮我看看这个原生聊天页结构是否正常。");
        m1.put("messageTime", new Date());
        messages.add(m1);

        Map<String, Object> m2 = new LinkedHashMap<>();
        m2.put("messageId", "m_ai_1");
        m2.put("role", "assistant");
        m2.put("content", "当前为 AI 对话页第一版骨架，后端接口已打通占位结构，后续将接入 OpenClaw Gateway 实现真实会话。 ");
        m2.put("messageTime", new Date());
        messages.add(m2);

        data.put("messages", messages);
        data.put("status", "mock");
        return success(data);
    }

    @PostMapping("/send")
    public AjaxResult send(@RequestBody Map<String, Object> req) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sessionId", req.get("sessionId"));
        data.put("requestId", "req_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        data.put("answer", "已收到消息：" + String.valueOf(req.get("message")) + "。当前后端仍是第一版 mock，下一步接 OpenClaw Gateway WebSocket。 ");
        data.put("responseTime", new Date());
        data.put("status", "mock");
        return success(data);
    }

    @PutMapping("/session/{sessionId}")
    public AjaxResult rename(@PathVariable String sessionId, @RequestBody Map<String, Object> req) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sessionId", sessionId);
        data.put("title", req.get("title"));
        return success(data);
    }

    @DeleteMapping("/session/{sessionId}")
    public AjaxResult delete(@PathVariable String sessionId) {
        return success("删除成功: " + sessionId);
    }
}
