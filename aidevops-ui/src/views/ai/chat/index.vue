<template>
  <div class="ai-chat-page">
    <div class="chat-wrapper">
      <!-- 诊断信息栏 -->
      <div v-if="diagnostics" class="diagnostics-bar">
        <div class="diag-item"><span>模式</span><strong>{{ diagnostics.mode || '-' }}</strong></div>
        <div class="diag-item"><span>WS</span><strong>{{ diagnostics.gatewayWsUrl ? '已配置' : '未配置' }}</strong></div>
        <div class="diag-item"><span>探测</span><strong :class="probeOk ? 'text-success' : 'text-muted'">{{ probeStage }}</strong></div>
        <div class="diag-item"><span>Token</span><strong :class="diagnostics.tokenConfigured ? 'text-success' : 'text-muted'">{{ diagnostics.tokenConfigured ? '已配置' : '未配置' }}</strong></div>
        <div class="diag-actions">
          <el-button size="small" :loading="probing" @click="handleProbe">重新探测</el-button>
          <el-button size="small" :loading="connecting" @click="handleConnectTest">Connect测试</el-button>
        </div>
      </div>

      <!-- 状态提示 -->
      <div class="status-bar">
        <span class="status-dot" :class="probeOk ? 'online' : 'offline'"></span>
        <span class="status-text">{{ statusText }}</span>
      </div>

      <!-- 消息区域 -->
      <div class="message-list" ref="messageList">
        <div v-if="!messages.length" class="empty-state">
          <div class="empty-icon">
            <svg viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg">
              <rect width="64" height="64" rx="16" fill="url(#chatGrad)"/>
              <path d="M20 26h24M20 34h16M20 42h20" stroke="white" stroke-width="2.5" stroke-linecap="round"/>
              <defs>
                <linearGradient id="chatGrad" x1="0" y1="0" x2="64" y2="64">
                  <stop stop-color="#3fa9ff"/>
                  <stop offset="1" stop-color="#215cff"/>
                </linearGradient>
              </defs>
            </svg>
          </div>
          <p>开始一次智能运维对话</p>
          <span>描述你遇到的运维问题，AI 将为你提供解决方案</span>
        </div>

        <div v-for="msg in messages" :key="msg.messageId || msg.id" class="message-row" :class="msg.role">
          <div v-if="msg.role !== 'user'" class="avatar assistant-avatar">
            <svg viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
              <rect width="32" height="32" rx="8" fill="url(#avGrad)"/>
              <circle cx="16" cy="13" r="4" fill="white"/>
              <path d="M10 24c0-3.3 2.7-6 6-6s6 2.7 6 6" stroke="white" stroke-width="2" stroke-linecap="round"/>
              <defs>
                <linearGradient id="avGrad" x1="0" y1="0" x2="32" y2="32">
                  <stop stop-color="#3fa9ff"/>
                  <stop offset="1" stop-color="#215cff"/>
                </linearGradient>
              </defs>
            </svg>
          </div>
          <div v-else class="avatar user-avatar">
            <svg viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
              <rect width="32" height="32" rx="8" fill="#2a3441"/>
              <circle cx="16" cy="13" r="4" fill="#8fd3ff"/>
              <path d="M10 24c0-3.3 2.7-6 6-6s6 2.7 6 6" stroke="#8fd3ff" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </div>
          <div class="message-bubble">
            <div class="message-role">{{ msg.role === 'user' ? '我' : 'AI 助手' }}</div>
            <div class="message-content">{{ msg.content }}</div>
          </div>
        </div>

        <div v-if="sending" class="message-row assistant">
          <div class="avatar assistant-avatar">
            <svg viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
              <rect width="32" height="32" rx="8" fill="url(#avGrad2)"/>
              <circle cx="16" cy="13" r="4" fill="white"/>
              <path d="M10 24c0-3.3 2.7-6 6-6s6 2.7 6 6" stroke="white" stroke-width="2" stroke-linecap="round"/>
              <defs>
                <linearGradient id="avGrad2" x1="0" y1="0" x2="32" y2="32">
                  <stop stop-color="#3fa9ff"/>
                  <stop offset="1" stop-color="#215cff"/>
                </linearGradient>
              </defs>
            </svg>
          </div>
          <div class="message-bubble typing">
            <span class="typing-dot"></span>
            <span class="typing-dot"></span>
            <span class="typing-dot"></span>
          </div>
        </div>
      </div>

      <!-- 输入区域 -->
      <div class="input-area">
        <div class="input-wrapper">
          <el-input
            v-model="inputMessage"
            type="textarea"
            :rows="3"
            resize="none"
            placeholder="输入你的运维问题，AI 将为你解答..."
            @keydown.native="handleInputKeydown"
          />
          <div class="input-actions">
            <span class="input-hint">Enter 发送 · Shift + Enter 换行</span>
            <div class="action-btns">
              <el-button @click="inputMessage = ''" size="small">清空</el-button>
              <el-button type="primary" :loading="sending" :disabled="!inputMessage.trim()" @click="handleSend" size="small">发送</el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Connect 草稿弹窗 -->
    <el-drawer title="Connect 调试信息" :visible.sync="showConnectDraft" size="480px" direction="rtl">
      <div v-if="connectDraft" class="draft-section">
        <div class="draft-title">Connect 请求草稿</div>
        <div class="draft-meta">
          <span>challenge: {{ connectDraft.challengeStage || '-' }}</span>
          <span>signature: {{ connectDraft.signatureReady ? 'ready' : 'pending' }}</span>
        </div>
        <pre class="draft-json">{{ formatJson(connectDraft.request || {}) }}</pre>
        <div v-if="connectDraft.signatureDraft" class="draft-title mt">待签名原文</div>
        <pre v-if="connectDraft.signatureDraft" class="draft-json">{{ connectDraft.signatureDraft.payloadJson || formatJson(connectDraft.signatureDraft.payload || {}) }}</pre>
        <div v-if="connectDraft.signatureResult" class="draft-title mt">签名器结果</div>
        <pre v-if="connectDraft.signatureResult" class="draft-json">{{ formatJson(connectDraft.signatureResult) }}</pre>
      </div>
      <div v-if="connectResult" class="draft-section">
        <div class="draft-title">Connect 测试结果</div>
        <pre class="draft-json">{{ formatJson(connectResult) }}</pre>
      </div>
    </el-drawer>
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
      if (!this.diagnostics) return '正在加载会话状态...'
      if (this.sending) return '消息发送中，请稍候...'
      if (this.connectResult && this.connectResult.ok) return 'Gateway connect 已打通，当前走真实会话收发'
      if (this.probeOk) return 'Gateway 已可达，可直接发送消息'
      if (this.diagnostics.enabled) return '已启用 Gateway，但当前探测还未成功'
      return '当前未启用真实 Gateway，对话先走本地回退'
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
      this.messages.push({ messageId: assistantId, role: 'assistant', content: '正在生成回复，请稍候...' })
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
.ai-chat-page {
  height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
}

.chat-wrapper {
  display: flex;
  flex-direction: column;
  height: 100%;
  max-width: 900px;
  margin: 0 auto;
  width: 100%;
  padding: 0 16px;
}

.diagnostics-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  margin-bottom: 12px;
  border-radius: 14px;
  background: rgba(255,255,255,0.03);
  border: 1px solid rgba(255,255,255,0.06);
  flex-wrap: wrap;
}

.diag-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  padding: 5px 10px;
  border-radius: 8px;
  background: rgba(255,255,255,0.04);
  span { opacity: 0.55; }
  strong { font-weight: 600; }
}

.diag-actions {
  margin-left: auto;
  display: flex;
  gap: 8px;
}

.text-success { color: #3fa9ff; }
.text-muted { color: rgba(255,255,255,0.35); }

.status-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  padding: 0 4px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  &.online { background: #3fa9ff; box-shadow: 0 0 8px rgba(63,169,255,0.5); }
  &.offline { background: rgba(255,255,255,0.25); }
}

.status-text {
  font-size: 13px;
  color: rgba(255,255,255,0.5);
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px 4px;
  display: flex;
  flex-direction: column;
  gap: 20px;

  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-track { background: transparent; }
  &::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.1); border-radius: 4px; }
}

.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 60px 20px;
  text-align: center;

  .empty-icon svg {
    width: 72px;
    height: 72px;
    opacity: 0.8;
  }

  p {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
    color: #eaf2ff;
  }

  span {
    font-size: 13px;
    color: rgba(255,255,255,0.4);
    max-width: 320px;
    line-height: 1.7;
  }
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;

  &.user {
    flex-direction: row-reverse;
    .message-bubble {
      background: linear-gradient(135deg, #3fa9ff 0%, #215cff 100%);
      border: none;
      border-bottom-right-radius: 6px;
    }
    .message-role { color: rgba(255,255,255,0.7); }
    .message-content { color: #fff; }
  }

  &.assistant {
    .message-bubble {
      background: rgba(255,255,255,0.06);
      border: 1px solid rgba(255,255,255,0.08);
      border-bottom-left-radius: 6px;
    }
  }
}

.avatar {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  border-radius: 10px;
  overflow: hidden;
  svg { width: 100%; height: 100%; }
}

.message-bubble {
  max-width: 72%;
  padding: 14px 16px;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.12);
}

.message-role {
  font-size: 11px;
  opacity: 0.6;
  margin-bottom: 6px;
  color: rgba(255,255,255,0.6);
}

.message-content {
  font-size: 14px;
  line-height: 1.8;
  color: #e4efff;
  white-space: pre-wrap;
  word-break: break-word;
}

.typing {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 16px 20px;
}

.typing-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #3fa9ff;
  animation: typingBounce 1.4s ease-in-out infinite;
  &:nth-child(2) { animation-delay: 0.2s; }
  &:nth-child(3) { animation-delay: 0.4s; }
}

@keyframes typingBounce {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.5; }
  30% { transform: translateY(-5px); opacity: 1; }
}

.input-area {
  padding: 16px 0 0;
  border-top: 1px solid rgba(255,255,255,0.06);
}

.input-wrapper {
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 18px;
  padding: 16px;
  transition: border-color 0.2s;

  &:focus-within {
    border-color: rgba(63,169,255,0.4);
    box-shadow: 0 0 0 3px rgba(63,169,255,0.08);
  }
}

.input-hint {
  font-size: 11px;
  color: rgba(255,255,255,0.3);
}

.input-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
}

.action-btns {
  display: flex;
  gap: 8px;
}

.draft-section {
  padding: 16px 20px;
}

.draft-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 10px;
  &.mt { margin-top: 20px; }
}

.draft-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: rgba(255,255,255,0.5);
  margin-bottom: 10px;
}

.draft-json {
  background: rgba(0,0,0,0.3);
  border-radius: 10px;
  padding: 14px;
  font-size: 12px;
  line-height: 1.7;
  color: #cde7ff;
  overflow-x: auto;
  margin: 0;
  max-height: 280px;
  overflow-y: auto;
  white-space: pre-wrap;
  word-break: break-all;
}

@media (max-width: 768px) {
  .message-bubble { max-width: 85%; }
  .diag-actions { margin-left: 0; width: 100%; }
  .input-hint { display: none; }
}
</style>
