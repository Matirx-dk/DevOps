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
          <div slot="header" class="chat-header">
            <span>{{ currentTitle || 'AI 运维对话' }}</span>
            <span class="chat-tip">当前为原生聊天页第一版骨架</span>
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
              placeholder="请输入消息，后续这里将直接接 OpenClaw 原生会话..."
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
import { createAiSession, listAiSession, getAiHistory, sendAiMessage } from '@/api/ai/chat'

export default {
  name: 'AiChatPage',
  data() {
    return {
      sessionList: [],
      currentSessionId: '',
      currentTitle: '',
      messages: [],
      inputMessage: '',
      sending: false
    }
  },
  created() {
    this.loadSessions()
  },
  methods: {
    loadSessions() {
      listAiSession().then(res => {
        this.sessionList = res.rows || []
        if (this.sessionList.length > 0) {
          this.handleSelectSession(this.sessionList[0])
        }
      })
    },
    handleCreateSession() {
      createAiSession({ title: '新会话', scene: 'ops' }).then(() => {
        this.loadSessions()
      })
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
      if (!this.inputMessage || !this.currentSessionId) return
      const text = this.inputMessage
      this.messages.push({
        messageId: 'local_' + Date.now(),
        role: 'user',
        content: text
      })
      this.inputMessage = ''
      this.sending = true
      this.$nextTick(() => this.scrollToBottom())
      sendAiMessage({ sessionId: this.currentSessionId, message: text, stream: false }).then(res => {
        this.messages.push({
          messageId: 'assistant_' + Date.now(),
          role: 'assistant',
          content: res.data ? res.data.answer : '暂无返回'
        })
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
.chat-tip {
  color: #8fd3ff;
  font-size: 12px;
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
  height: 540px;
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
