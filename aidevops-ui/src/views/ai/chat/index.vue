<template>
  <div class="ai-chat-page app-container">
    <el-row :gutter="20">
      <el-col :xs="24" :lg="24">
        <el-card shadow="hover" class="chat-card message-card">
          <div slot="header" class="chat-header header-main">
            <div>
              <div class="header-title">{{ currentTitle || 'AI 运维对话' }}</div>
              <div class="chat-tip">{{ statusText }}</div>
            </div>
            <div class="header-actions">
              <el-button size="mini" :loading="probing" @click="handleProbe">重新探测</el-button>
              <el-button size="mini" :loading="connecting" @click="handleConnectTest">connect 测试</el-button>
              <el-button size="mini" @click="showConnectDraft = !showConnectDraft">{{ showConnectDraft ? '收起草稿' : '查看 connect 草稿' }}</el-button>
              <el-tag size="mini" :type="probeOk ? 'success' : 'info'">{{ probeOk ? 'WS可达' : '本地回退' }}</el-tag>
            </div>
          </div>

          <div v-if="diagnostics" class="diagnostics-bar">
            <div class="diag-item"><span>模式</span><strong>{{ diagnostics.mode || '-' }}</strong></div>
            <div class="diag-item"><span>WS</span><strong>{{ diagnostics.gatewayWsUrl || '-' }}</strong></div>
            <div class="diag-item"><span>探测</span><strong>{{ probeStage }}</strong></div>
            <div class="diag-item"><span>Token</span><strong>{{ diagnostics.tokenConfigured ? '已配置' : '未配置' }}</strong></div>
            <div class="diag-item"><span>Signer</span><strong>{{ diagnostics.experimentalSignerEnabled ? '实验Ed25519' : '占位' }}</strong></div>
          </div>

          <div v-if="showConnectDraft && connectDraft" class="connect-draft-panel">
            <div class="draft-title">Connect 请求草稿</div>
            <div class="draft-meta">
              <span>challenge: {{ connectDraft.challengeStage || '-' }}</span>
              <span>signature: {{ connectDraft.signatureReady ? 'ready' : 'pending' }}</span>
            </div>
            <pre class="draft-json">{{ formatJson(connectDraft.request || {}) }}</pre>
            <div v-if="connectDraft.signatureDraft" class="draft-subtitle">待签名原文</div>
            <pre v-if="connectDraft.signatureDraft" class="draft-json">{{ connectDraft.signatureDraft.payloadJson || formatJson(connectDraft.signatureDraft.payload || {}) }}</pre>
            <div v-if="connectDraft.signatureResult" class="draft-subtitle">签名器结果</div>
            <pre v-if="connectDraft.signatureResult" class="draft-json">{{ formatJson(connectDraft.signatureResult) }}</pre>
          </div>

          <div v-if="connectResult" class="connect-draft-panel">
            <div class="draft-title">Connect 测试结果</div>
            <div class="draft-meta">
              <span>stage: {{ connectResult.stage || '-' }}</span>
              <span>ok: {{ connectResult.ok ? 'true' : 'false' }}</span>
            </div>
            <div v-if="connectResult.summary" class="draft-subtitle">结果摘要</div>
            <pre v-if="connectResult.summary" class="draft-json">{{ formatJson(connectResult.summary) }}</pre>
            <pre class="draft-json">{{ formatJson(connectResult) }}</pre>
          </div>

          <div class="message-list" ref="messageList">
            <div v-for="msg in messages" :key="msg.messageId || msg.id" class="message-row" :class="msg.role">
              <div class="message-bubble">
                <div class="message-role">{{ msg.role === 'user' ? '我' : 'OpenClaw' }}</div>
                <div class="message-content">{{ msg.content }}</div>
              </div>
            </div>
          </div>

          <div class="message-input-bar">
            <el-input
              v-model="inputMessage"
              type="textarea"
              :rows="3"
              resize="none"
              placeholder="输入消息，按 Enter 发送，Shift + Enter 换行"
              @keydown.native="handleInputKeydown"
            />
            <div class="message-actions">
              <el-button @click="inputMessage = ''">清空</el-button>
              <el-button type="primary" :loading="sending" @click="handleSend">发送</el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import {
  createAiSession,
  listAiSession,
  getAiHistory,
  sendAiMessage,
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
        const answer = res.data && res.data.answer ? res.data.answer : '暂无返回'
        const idx = this.messages.findIndex(item => item.messageId === assistantId)
        if (idx >= 0) {
          this.$set(this.messages, idx, { messageId: assistantId, role: 'assistant', content: answer })
        }
        this.loadSessions()
        this.$nextTick(() => this.scrollToBottom())
      }).catch(() => {
        const idx = this.messages.findIndex(item => item.messageId === assistantId)
        if (idx >= 0) {
          this.$set(this.messages, idx, { messageId: assistantId, role: 'assistant', content: '请求超时或发送失败，请重试。' })
        }
      }).finally(() => {
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
.ai-chat-page { min-height: calc(100vh - 120px); }
.chat-card { margin-bottom: 20px; border-radius: 18px; }
.chat-header { display: flex; align-items: center; justify-content: space-between; }
.header-main { gap: 12px; }
.header-actions { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.header-title { font-size: 18px; font-weight: 700; }
.chat-tip { color: #8fd3ff; font-size: 12px; margin-top: 4px; }
.diagnostics-bar { display: flex; gap: 10px; margin-bottom: 14px; flex-wrap: wrap; }
.diag-item { padding: 8px 12px; border-radius: 12px; background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.08); font-size: 12px; }
.diag-item span { opacity: 0.65; margin-right: 8px; }
.connect-draft-panel { margin-bottom: 16px; padding: 14px; border-radius: 14px; background: rgba(15, 23, 42, 0.55); border: 1px solid rgba(143, 211, 255, 0.16); }
.draft-title { font-size: 14px; font-weight: 600; margin-bottom: 8px; }
.draft-meta { display: flex; gap: 14px; font-size: 12px; color: rgba(234, 242, 255, 0.72); margin-bottom: 10px; }
.draft-subtitle { font-size: 13px; font-weight: 600; margin: 12px 0 8px; color: #8fd3ff; }
.draft-json { margin: 0; max-height: 240px; overflow: auto; padding: 12px; border-radius: 12px; background: rgba(2, 6, 23, 0.72); color: #cde7ff; font-size: 12px; line-height: 1.6; }
.message-list { height: 520px; overflow-y: auto; padding: 12px 4px; border-radius: 16px; background: linear-gradient(180deg, rgba(255,255,255,0.02), rgba(255,255,255,0.04)); }
.message-row { display: flex; margin-bottom: 16px; }
.message-row.user { justify-content: flex-end; }
.message-row.assistant { justify-content: flex-start; }
.message-bubble { max-width: 78%; padding: 14px 16px; border-radius: 18px; background: rgba(255,255,255,0.06); border: 1px solid rgba(255,255,255,0.08); box-shadow: 0 6px 18px rgba(0,0,0,0.08); }
.message-row.user .message-bubble { background: linear-gradient(135deg, #3fa9ff 0%, #215cff 100%); border: none; }
.message-role { font-size: 12px; opacity: 0.7; margin-bottom: 8px; }
.message-content { line-height: 1.9; white-space: pre-wrap; word-break: break-word; }
.message-input-bar { margin-top: 18px; padding-top: 18px; border-top: 1px solid rgba(255,255,255,0.08); }
.message-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 12px; }

@media (max-width: 768px) {
  .message-list { height: 440px; }
  .message-bubble { max-width: 88%; }
}
</style>
