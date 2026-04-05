<template>
  <div class="ai-chat-page app-container">
    <el-card shadow="hover" class="chat-card">
      <div slot="header" class="chat-header">
        <div>
          <div class="header-title">{{ currentTitle || 'AI 运维对话' }}</div>
          <div class="chat-tip">{{ statusText }}</div>
        </div>
        <div class="header-actions">
          <el-button size="mini" :loading="probing" @click="handleProbe">重新探测</el-button>
          <el-button size="mini" :loading="connecting" @click="handleConnectTest">Connect测试</el-button>
          <el-tag size="mini" :type="probeOk ? 'success' : 'info'">{{ probeOk ? 'WS可达' : '本地回退' }}</el-tag>
        </div>
      </div>

      <div v-if="diagnostics" class="diagnostics-bar">
        <div class="diag-item"><span>模式</span><strong>{{ diagnostics.mode || '-' }}</strong></div>
        <div class="diag-item"><span>WS</span><strong>{{ diagnostics.gatewayWsUrl || '-' }}</strong></div>
        <div class="diag-item"><span>探测</span><strong>{{ probeStage }}</strong></div>
        <div class="diag-item"><span>Token</span><strong>{{ diagnostics.tokenConfigured ? '已配置' : '未配置' }}</strong></div>
      </div>

      <div class="message-list" ref="messageList">
        <div v-for="msg in messages" :key="msg.messageId || msg.id" class="message-row" :class="msg.role">
          <div class="message-bubble">
            <div class="message-role">{{ msg.role === 'user' ? '我' : 'AI 助手' }}</div>
            <div class="message-content">{{ msg.content }}</div>
          </div>
        </div>
        <div v-if="sending" class="message-row assistant">
          <div class="message-bubble">
            <div class="message-role">AI 助手</div>
            <div class="message-content">正在生成回复，请稍候<span class="blink">...</span></div>
          </div>
        </div>
      </div>

      <div class="message-input-bar">
        <el-input
          v-model="inputMessage"
          type="textarea"
          :rows="3"
          resize="none"
          placeholder="输入消息，Enter 发送，Shift + Enter 换行"
          @keydown.native="handleInputKeydown"
        />
        <div class="message-actions">
          <el-button @click="inputMessage = ''" size="small">清空</el-button>
          <el-button type="primary" :loading="sending" :disabled="!inputMessage.trim()" @click="handleSend" size="small">发送</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>
import {
  createAiSession,
  listAiSession,
  getAiHistory,
  sendAiMessage,
  getAiSendResult,
  getAiGatewayDiagnostics,
  probeAiGateway,
  getAiConnectDraft,
  testAiConnect
} from '@/api/ai/chat'

export default {
  name: 'AiChatPage',
  data() {
    return {
      sessionList: [],
      currentSessionId: '',
      currentTitle: '',
      messages: [],
      diagnostics: null,
      connectDraft: null,
      connectResult: null,
      showConnectDraft: false,
      inputMessage: '',
      sending: false,
      probing: false,
      connecting: false
    }
  },
  computed: {
    probeOk() {
      return !!(this.diagnostics && this.diagnostics.probe && this.diagnostics.probe.ok)
    },
    probeStage() {
      return (this.diagnostics && this.diagnostics.probe && this.diagnostics.probe.stage) || '-'
    },
    statusText() {
      if (!this.diagnostics) return '正在连接 AI 服务...'
      if (this.sending) return '消息发送中，请稍候...'
      if (this.connectResult && this.connectResult.ok) return 'AI 服务已就绪'
      if (this.probeOk) return 'AI 服务已就绪'
      if (this.diagnostics.enabled) return 'AI 服务连接中...'
      return 'AI 服务连接中...'
    }
  },
  created() {
    this.loadSessions()
    this.loadDiagnostics()
    this.loadConnectDraft()
  },
  methods: {
    formatJson(obj) {
      return JSON.stringify(obj, null, 2)
    },
    loadSessions() {
      listAiSession().then(res => {
        this.sessionList = res.rows || []
        if (this.sessionList.length > 0 && !this.currentSessionId) {
          this.handleSelectSession(this.sessionList[0])
        }
      })
    },
    loadDiagnostics() {
      getAiGatewayDiagnostics().then(res => {
        this.diagnostics = res.data || null
      })
    },
    loadConnectDraft() {
      getAiConnectDraft().then(res => {
        this.connectDraft = res.data || null
      })
    },
    handleProbe() {
      this.probing = true
      probeAiGateway().then(res => {
        const data = res.data || {}
        this.diagnostics = Object.assign({}, this.diagnostics || {}, {
          mode: data.mode || (this.diagnostics && this.diagnostics.mode),
          probe: data.probe || null
        })
        this.loadConnectDraft()
        this.$message.success('探测已完成')
      }).finally(() => {
        this.probing = false
      })
    },
    handleConnectTest() {
      this.connecting = true
      testAiConnect().then(res => {
        this.connectResult = res.data || null
        this.showConnectDraft = true
        this.loadDiagnostics()
        this.loadConnectDraft()
        this.$message.success('connect 测试已返回')
      }).finally(() => {
        this.connecting = false
      })
    },
    handleCreateSession() {
      createAiSession({ title: '新会话', scene: 'ops' }).then(res => {
        const created = res.data || {}
        this.currentSessionId = created.sessionId || ''
        this.currentTitle = created.title || '新会话'
        this.loadSessions()
        if (this.currentSessionId) {
          this.handleSelectSession({ sessionId: this.currentSessionId, title: this.currentTitle })
        }
      })
    },
    handleInputKeydown(e) {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault()
        this.handleSend()
      }
    },
    handleSelectSession(item) {
      this.currentSessionId = item.sessionId
      this.currentTitle = item.title
      getAiHistory(item.sessionId).then(res => {
        this.messages = (res.data && res.data.messages) || []
        this.diagnostics = res.data ? res.data.diagnostics : this.diagnostics
        this.$nextTick(() => this.scrollToBottom())
      })
    },
    handleSend() {
      if (this.sending || !this.inputMessage || !this.currentSessionId) return
      const text = this.inputMessage.trim()
      if (!text) return
      const userId = 'local_user_' + Date.now()
      const assistantId = 'local_assistant_' + (Date.now() + 1)
      this.messages.push({ messageId: userId, role: 'user', content: text })
      this.messages.push({ messageId: assistantId, role: 'assistant', content: '您好，我是 AI 运维助手。\n\n我可以帮你：\n- 解答服务器、容器、Kubernetes 相关问题\n- 分析日志和错误信息\n- 提供 CI/CD 流水线配置建议\n- 协助排查网络、存储、性能问题\n- 编写或优化运维脚本\n\n请描述你遇到的问题，我来帮你分析。' })
      this.inputMessage = ''
      this.sending = true
      this.$nextTick(() => this.scrollToBottom())
      sendAiMessage({ sessionId: this.currentSessionId, message: text, stream: false }).then(res => {
        const runId = res.data && res.data.runId
        if (!runId) {
          throw new Error('missing runId')
        }
        const poll = (count = 0) => {
          getAiSendResult(this.currentSessionId, runId).then(result => {
            const data = result.data || {}
            if (data.status === 'completed' || data.status === 'failed') {
              const answer = data.answer || (data.status === 'failed' ? '请求失败，请重试。' : '暂无返回')
              const idx = this.messages.findIndex(item => item.messageId === assistantId)
              if (idx >= 0) {
                this.$set(this.messages, idx, { messageId: assistantId, role: 'assistant', content: answer })
              }
              this.loadSessions()
              this.$nextTick(() => this.scrollToBottom())
              this.sending = false
              return
            }
            if (count >= 59) {
              throw new Error('poll timeout')
            }
            setTimeout(() => poll(count + 1), 1000)
          }).catch(() => {
            const idx = this.messages.findIndex(item => item.messageId === assistantId)
            if (idx >= 0) {
              this.$set(this.messages, idx, { messageId: assistantId, role: 'assistant', content: '请求超时或发送失败，请重试。' })
            }
            this.sending = false
          })
        }
        poll()
      }).catch(() => {
        const idx = this.messages.findIndex(item => item.messageId === assistantId)
        if (idx >= 0) {
          this.$set(this.messages, idx, { messageId: assistantId, role: 'assistant', content: '请求超时或发送失败，请重试。' })
        }
        this.sending = false
      })
    },
    scrollToBottom() {
      const el = this.$refs.messageList
      if (el) el.scrollTop = el.scrollHeight
    }
  }
}
</script>

<style lang="scss" scoped>
.chat-card {
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.97);
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.header-title {
  font-size: 18px;
  font-weight: 700;
  color: #1a1a2e;
}

.chat-tip {
  color: #888;
  font-size: 12px;
  margin-top: 4px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.diagnostics-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  padding: 10px 14px;
  background: #f8f9fa;
  border-radius: 10px;
}

.diag-item {
  font-size: 12px;
  color: #555;
  span { opacity: 0.6; margin-right: 4px; }
  strong { font-weight: 600; }
}

.message-list {
  height: 480px;
  overflow-y: auto;
  padding: 8px 4px;
  background: #f8f9fa;
  border-radius: 12px;
  margin-bottom: 16px;
}

.message-row {
  display: flex;
  margin-bottom: 14px;
  &.user { justify-content: flex-end; }
  &.assistant { justify-content: flex-start; }
}

.message-bubble {
  max-width: 72%;
  padding: 12px 16px;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}

.message-row.user .message-bubble {
  background: linear-gradient(135deg, #3fa9ff 0%, #215cff 100%);
  .message-role { color: rgba(255,255,255,0.7); }
  .message-content { color: #fff; }
}

.message-role {
  font-size: 11px;
  color: #999;
  margin-bottom: 6px;
}

.message-content {
  font-size: 14px;
  line-height: 1.8;
  color: #333;
  white-space: pre-wrap;
  word-break: break-word;
}

.blink {
  animation: blink 1s step-start infinite;
}

@keyframes blink {
  50% { opacity: 0; }
}

.message-input-bar {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.message-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
