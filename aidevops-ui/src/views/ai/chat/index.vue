<template>
  <div class="ai-chat-page app-container">
    <el-row :gutter="20">
      <el-col :xs="24" :lg="5">
        <el-card shadow="hover" class="chat-card session-card">
          <div slot="header" class="chat-header">
            <span>AI 对话</span>
            <el-button type="primary" size="mini" @click="handleCreateSession">新建会话</el-button>
          </div>
          <div class="session-list">
            <div
              v-for="item in sessionList"
              :key="item.sessionId"
              class="session-item"
              :class="{ active: currentSessionId === item.sessionId }"
              @click="handleSelectSession(item)"
            >
              <div class="session-title">{{ item.title }}</div>
              <div class="session-last">{{ item.lastMessage }}</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="19">
        <el-card shadow="hover" class="chat-card message-card">
          <div slot="header" class="chat-header header-main">
            <div>
              <div class="header-title">{{ currentTitle || 'AI 运维对话' }}</div>
              <div class="chat-tip">{{ statusText }}</div>
            </div>
            <div class="header-actions">
              <el-button size="mini" :loading="probing" @click="handleProbe">重新探测</el-button>
              <el-button size="mini" @click="showConnectDraft = !showConnectDraft">{{ showConnectDraft ? '收起草稿' : '查看 connect 草稿' }}</el-button>
              <el-tag size="mini" :type="probeOk ? 'success' : 'info'">{{ probeOk ? 'WS可达' : '本地回退' }}</el-tag>
            </div>
          </div>

          <div v-if="diagnostics" class="diagnostics-bar">
            <div class="diag-item"><span>模式</span><strong>{{ diagnostics.mode || '-' }}</strong></div>
            <div class="diag-item"><span>WS</span><strong>{{ diagnostics.gatewayWsUrl || '-' }}</strong></div>
            <div class="diag-item"><span>探测</span><strong>{{ probeStage }}</strong></div>
            <div class="diag-item"><span>Token</span><strong>{{ diagnostics.tokenConfigured ? '已配置' : '未配置' }}</strong></div>
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
              :rows="4"
              resize="none"
              placeholder="请输入消息，当前会先做 Gateway challenge 探测，再继续推进真实对接..."
              @keyup.ctrl.enter.native="handleSend"
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
  getAiConnectDraft
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
      showConnectDraft: false,
      inputMessage: '',
      sending: false,
      probing: false
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
      if (this.probeOk) return '当前已拿到 Gateway challenge，下一步是 device 签名 + connect'
      if (this.diagnostics.enabled) return '已启用 Gateway 探测，但当前仍未探测成功'
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
        if (data.message) {
          this.$modal && this.$modal.msgSuccess ? this.$modal.msgSuccess('探测已完成') : this.$message.success('探测已完成')
        }
      }).finally(() => {
        this.probing = false
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
      if (!this.inputMessage || !this.currentSessionId) return
      const text = this.inputMessage.trim()
      if (!text) return
      this.inputMessage = ''
      this.sending = true
      sendAiMessage({ sessionId: this.currentSessionId, message: text, stream: false }).then(res => {
        const answer = res.data ? res.data.answer : '暂无返回'
        this.messages.push({
          messageId: 'local_user_' + Date.now(),
          role: 'user',
          content: text
        })
        this.messages.push({
          messageId: 'local_assistant_' + (Date.now() + 1),
          role: 'assistant',
          content: answer
        })
        if (res.data && res.data.probe) {
          this.diagnostics = Object.assign({}, this.diagnostics || {}, { probe: res.data.probe })
        }
        if (res.data && res.data.connectDraft) {
          this.connectDraft = res.data.connectDraft
        }
        this.loadSessions()
        this.$nextTick(() => this.scrollToBottom())
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
.ai-chat-page {
  min-height: calc(100vh - 120px);
}
.chat-card {
  margin-bottom: 20px;
  border-radius: 18px;
}
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.header-main {
  gap: 12px;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.header-title {
  font-size: 16px;
  font-weight: 600;
}
.chat-tip {
  color: #8fd3ff;
  font-size: 12px;
  margin-top: 4px;
}
.diagnostics-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}
.diag-item {
  padding: 8px 12px;
  border-radius: 12px;
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.08);
  font-size: 12px;
}
.diag-item span {
  opacity: 0.65;
  margin-right: 8px;
}
.connect-draft-panel {
  margin-bottom: 16px;
  padding: 14px;
  border-radius: 14px;
  background: rgba(15, 23, 42, 0.55);
  border: 1px solid rgba(143, 211, 255, 0.16);
}
.draft-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 8px;
}
.draft-meta {
  display: flex;
  gap: 14px;
  font-size: 12px;
  color: rgba(234, 242, 255, 0.72);
  margin-bottom: 10px;
}
.draft-subtitle {
  font-size: 13px;
  font-weight: 600;
  margin: 12px 0 8px;
  color: #8fd3ff;
}
.draft-json {
  margin: 0;
  max-height: 240px;
  overflow: auto;
  padding: 12px;
  border-radius: 12px;
  background: rgba(2, 6, 23, 0.72);
  color: #cde7ff;
  font-size: 12px;
  line-height: 1.6;
}
.session-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.session-item {
  padding: 14px;
  border-radius: 14px;
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.08);
  cursor: pointer;
}
.session-item.active {
  border-color: rgba(63,169,255,0.4);
  box-shadow: inset 0 0 0 1px rgba(63,169,255,0.16);
}
.session-title {
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
  margin-bottom: 8px;
}
.session-last {
  font-size: 12px;
  color: rgba(234, 242, 255, 0.66);
  line-height: 1.7;
}
.message-list {
  height: 460px;
  overflow-y: auto;
  padding: 8px 2px;
}
.message-row {
  display: flex;
  margin-bottom: 14px;
}
.message-row.user {
  justify-content: flex-end;
}
.message-row.assistant {
  justify-content: flex-start;
}
.message-bubble {
  max-width: 82%;
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(255,255,255,0.06);
  border: 1px solid rgba(255,255,255,0.08);
}
.message-row.user .message-bubble {
  background: linear-gradient(135deg, #3fa9ff 0%, #215cff 100%);
  border: none;
}
.message-role {
  font-size: 12px;
  opacity: 0.7;
  margin-bottom: 8px;
}
.message-content {
  line-height: 1.9;
  white-space: pre-wrap;
  word-break: break-word;
}
.message-input-bar {
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid rgba(255,255,255,0.08);
}
.message-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 14px;
}
</style>
