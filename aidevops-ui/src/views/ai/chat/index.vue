<template>
  <div class="ai-chat-page app-container">
    <el-card shadow="hover" class="chat-card">
      <!-- Header -->
      <div slot="header" class="chat-header">
        <div class="header-left">
          <div class="header-title">
            <i class="el-icon-connection" style="margin-right:8px;color:#3fa9ff"></i>
            {{ currentTitle || 'AI 运维对话' }}
          </div>
          <div class="chat-tip">
            <span v-if="sending" class="status-dot sending">●</span>
            <span v-else class="status-dot ready">●</span>
            {{ statusText }}
          </div>
        </div>
        <div class="header-right">
          <el-button size="mini" round @click="handleNewSession">
            <i class="el-icon-plus"></i> 新对话
          </el-button>
        </div>
      </div>

      <!-- Message List -->
      <div class="message-list" ref="messageList">
        <div v-if="messages.length === 0" class="empty-state">
          <div class="empty-icon">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z" fill="#3fa9ff" opacity="0.3"/>
              <circle cx="12" cy="12" r="10" stroke="#3fa9ff" stroke-width="1.5" fill="none" opacity="0.5"/>
              <path d="M8 12h8M12 8v8" stroke="#3fa9ff" stroke-width="1.5" stroke-linecap="round" opacity="0.7"/>
            </svg>
          </div>
          <div class="empty-title">开始一段新对话</div>
          <div class="empty-desc">描述你遇到的运维问题，AI 助手会尽力解答</div>
          <div class="quick-prompts">
            <el-tag v-for="q in quickPrompts" :key="q" size="small" class="quick-tag" @click="handleQuickPrompt(q)">{{ q }}</el-tag>
          </div>
        </div>

        <div v-for="msg in messages" :key="msg.messageId || msg.id" class="message-row" :class="msg.role">
          <div class="avatar">
            <div v-if="msg.role === 'user'" class="avatar-user">我</div>
            <div v-else class="avatar-ai">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/></svg>
            </div>
          </div>
          <div class="message-bubble" :class="msg.role">
            <div class="message-content" v-html="formatContent(msg.content)"></div>
          </div>
        </div>

        <div v-if="sending && messages.length > 0" class="message-row assistant">
          <div class="avatar">
            <div class="avatar-ai">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/></svg>
            </div>
          </div>
          <div class="message-bubble assistant typing">
            <div class="typing-dots"><span></span><span></span><span></span></div>
          </div>
        </div>
      </div>

      <!-- Input Bar -->
      <div class="input-bar">
        <div class="input-area" :class="{ focus: inputFocused }">
          <el-input
            v-model="inputMessage"
            type="textarea"
            :rows="3"
            resize="none"
            placeholder="输入消息，Enter 发送，Shift + Enter 换行"
            @keydown.enter.exact.prevent="handleInputKeydown"
            @focus="inputFocused = true"
            @blur="inputFocused = false"
          />
        </div>
        <div class="input-actions">
          <span class="input-hint">Enter 发送 · Shift+Enter 换行</span>
          <div class="action-btns">
            <el-button size="small" @click="inputMessage = ''" :disabled="!inputMessage">清空</el-button>
            <el-button type="primary" size="small" :loading="sending" :disabled="!inputMessage.trim() || !currentSessionId" @click="handleSend">
              {{ sending ? '发送中...' : '发送' }}
            </el-button>
          </div>
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
  getAiGatewayDiagnostics
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
      inputMessage: '',
      sending: false,
      inputFocused: false,
      quickPrompts: [
        '帮我分析一下服务器 CPU 使用率高的原因',
        'K8s Pod 一直处于 Pending 状态怎么排查',
        '如何配置 Jenkins 流水线实现自动部署',
        'Harbor 镜像拉取失败怎么解决'
      ]
    }
  },
  computed: {
    probeOk() {
      return !!(this.diagnostics && this.diagnostics.probe && this.diagnostics.probe.ok)
    },
    statusText() {
      if (this.sending) return 'AI 正在思考...'
      if (this.probeOk) return '在线'
      if (this.diagnostics && this.diagnostics.enabled) return '连接中...'
      if (this.currentSessionId) return '就绪'
      return '请选择或新建会话'
    }
  },
  created() {
    this.loadDiagnostics()
    this.loadSessions()
  },
  methods: {
    formatJson(obj) {
      return JSON.stringify(obj, null, 2)
    },
    formatContent(content) {
      if (!content) return ''
      // 简单处理换行和代码块
      return content
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/```([\s\S]*?)```/g, '<pre class="code-block">$1</pre>')
        .replace(/`([^`]+)`/g, '<code class="inline-code">$1</code>')
        .replace(/\n/g, '<br>')
    },
    loadDiagnostics() {
      getAiGatewayDiagnostics().then(res => {
        this.diagnostics = res.data || null
      })
    },
    loadSessions() {
      listAiSession().then(res => {
        this.sessionList = res.rows || []
        if (this.sessionList.length > 0) {
          this.handleSelectSession(this.sessionList[0])
        }
      })
    },
    handleQuickPrompt(text) {
      if (!this.currentSessionId) {
        this.handleNewSession().then(() => {
          this.inputMessage = text
          this.$nextTick(() => this.handleSend())
        })
      } else {
        this.inputMessage = text
        this.$nextTick(() => this.handleSend())
      }
    },
    handleNewSession() {
      return createAiSession({ title: '新会话', scene: 'ops' }).then(res => {
        const created = res.data || {}
        this.currentSessionId = created.sessionId || ''
        this.currentTitle = created.title || '新会话'
        this.messages = []
        this.loadSessions()
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
      this.messages.push({ messageId: assistantId, role: 'assistant', content: '' })
      this.inputMessage = ''
      this.sending = true
      this.$nextTick(() => this.scrollToBottom())

      sendAiMessage({ sessionId: this.currentSessionId, message: text, stream: false }).then(res => {
        const runId = res.data && res.data.runId
        if (!runId) {
          this.updateMessage(assistantId, '请求失败，请重试。')
          this.sending = false
          return
        }
        const poll = (count = 0) => {
          getAiSendResult(this.currentSessionId, runId).then(result => {
            const data = result.data || {}
            if (data.status === 'completed') {
              this.updateMessage(assistantId, data.answer || '暂无返回')
              this.sending = false
              this.$nextTick(() => this.scrollToBottom())
              return
            }
            if (data.status === 'failed') {
              this.updateMessage(assistantId, '请求处理失败，请重试。')
              this.sending = false
              this.$nextTick(() => this.scrollToBottom())
              return
            }
            if (count >= 59) {
              this.updateMessage(assistantId, '请求超时，请重试。')
              this.sending = false
              this.$nextTick(() => this.scrollToBottom())
              return
            }
            setTimeout(() => poll(count + 1), 1000)
          }).catch(() => {
            this.updateMessage(assistantId, '网络异常，请检查连接后重试。')
            this.sending = false
            this.$nextTick(() => this.scrollToBottom())
          })
        }
        poll()
      }).catch(() => {
        this.updateMessage(assistantId, '发送失败，请重试。')
        this.sending = false
        this.$nextTick(() => this.scrollToBottom())
      })
    },
    updateMessage(id, content) {
      const idx = this.messages.findIndex(item => item.messageId === id)
      if (idx >= 0) {
        this.$set(this.messages, idx, { messageId: id, role: 'assistant', content })
      }
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
  height: calc(100vh - 140px);
  display: flex;
  flex-direction: column;
}

.chat-card {
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.97);
  display: flex;
  flex-direction: column;
  height: 100%;
  ::v-deep .el-card__body {
    display: flex;
    flex-direction: column;
    height: calc(100% - 60px);
    overflow: hidden;
  }
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f2f5;
}

.header-left {
  .header-title {
    font-size: 16px;
    font-weight: 600;
    color: #1a1a2e;
    display: flex;
    align-items: center;
  }
  .chat-tip {
    color: #999;
    font-size: 12px;
    margin-top: 4px;
    display: flex;
    align-items: center;
    gap: 4px;
    .status-dot {
      font-size: 10px;
      &.ready { color: #52c41a; }
      &.sending { color: #faad14; }
    }
  }
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px 8px;
  background: #f7f8fa;
  border-radius: 12px;
  margin: 12px 0;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  text-align: center;
  .empty-icon {
    margin-bottom: 16px;
    opacity: 0.8;
  }
  .empty-title {
    font-size: 16px;
    font-weight: 600;
    color: #333;
    margin-bottom: 8px;
  }
  .empty-desc {
    font-size: 13px;
    color: #999;
    margin-bottom: 20px;
  }
  .quick-prompts {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    justify-content: center;
    .quick-tag {
      cursor: pointer;
      &:hover { background: #3fa9ff; color: #fff; border-color: #3fa9ff; }
    }
  }
}

.message-row {
  display: flex;
  align-items: flex-start;
  margin-bottom: 16px;
  gap: 10px;
  &.user {
    flex-direction: row-reverse;
    .message-bubble { background: linear-gradient(135deg, #3fa9ff 0%, #0066ff 100%); }
    .message-content { color: #fff; }
  }
  &.assistant {
    flex-direction: row;
    .message-bubble { background: #fff; }
    .message-content { color: #333; }
  }
}

.avatar {
  flex-shrink: 0;
  .avatar-user, .avatar-ai {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    font-weight: 600;
  }
  .avatar-user {
    background: linear-gradient(135deg, #3fa9ff, #0066ff);
    color: #fff;
  }
  .avatar-ai {
    background: #f0f7ff;
    color: #3fa9ff;
  }
}

.message-bubble {
  max-width: 72%;
  padding: 12px 16px;
  border-radius: 16px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  .message-content {
    font-size: 14px;
    line-height: 1.8;
    white-space: pre-wrap;
    word-break: break-word;
    ::v-deep .code-block {
      background: #1e1e1e;
      color: #d4d4d4;
      border-radius: 8px;
      padding: 12px 16px;
      margin: 8px 0;
      font-family: 'Courier New', monospace;
      font-size: 13px;
      overflow-x: auto;
      white-space: pre;
    }
    ::v-deep .inline-code {
      background: #f0f0f0;
      color: #c7254e;
      padding: 2px 6px;
      border-radius: 4px;
      font-family: 'Courier New', monospace;
      font-size: 13px;
    }
  }
  &.typing {
    padding: 14px 18px;
  }
}

.typing-dots {
  display: flex;
  gap: 4px;
  span {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #3fa9ff;
    animation: typing 1.2s ease-in-out infinite;
    &:nth-child(2) { animation-delay: 0.2s; }
    &:nth-child(3) { animation-delay: 0.4s; }
  }
}

@keyframes typing {
  0%, 100% { opacity: 0.3; transform: translateY(0); }
  50% { opacity: 1; transform: translateY(-4px); }
}

.input-bar {
  .input-area {
    border-radius: 12px;
    border: 1.5px solid #e8e8e8;
    overflow: hidden;
    transition: border-color 0.2s;
    &.focus { border-color: #3fa9ff; }
    ::v-deep .el-textarea__inner {
      border: none;
      border-radius: 0;
      padding: 12px 14px;
      font-size: 14px;
      line-height: 1.6;
      &:focus { box-shadow: none; }
    }
  }
  .input-actions {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: 10px;
    .input-hint {
      font-size: 12px;
      color: #ccc;
    }
    .action-btns {
      display: flex;
      gap: 8px;
    }
  }
}
</style>
