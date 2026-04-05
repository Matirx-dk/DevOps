<template>
  <div class="ops-center app-container">
    <el-row :gutter="20">
      <el-col :xs="24" :lg="10">
        <el-card shadow="hover" class="ops-card ai-card">
          <div slot="header" class="ops-header">
            <span>AI 对话</span>
            <span class="ops-tag">OpenClaw</span>
          </div>
          <div class="ai-entry">
            <div class="ai-entry-title">运维协同对话入口</div>
            <p class="ai-entry-desc">在平台内直接打开 OpenClaw 对话窗口，用于巡检、排障、发布协助和日常运维沟通。</p>
            <div class="ai-actions">
              <router-link to="/ai/chat"><el-button type="primary" size="mini">打开对话框</el-button></router-link>
              <el-link href="https://devops.zoudekang.cloud/openclaw/" target="_blank" type="primary">新标签打开</el-link>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="7">
        <el-card shadow="hover" class="ops-card">
          <div slot="header"><span>配置与发布</span></div>
          <div class="ops-links">
            <el-link href="https://devops.zoudekang.cloud/nacos/" target="_blank">Nacos 配置中心</el-link>
            <el-link href="https://devops.zoudekang.cloud/jenkins/" target="_blank">Jenkins 流水线</el-link>
            <el-link href="/swagger-ui/index.html" target="_blank">Swagger 接口文档</el-link>
            <router-link to="/ai/chat"><el-link>AI 对话入口</el-link></router-link>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="7">
        <el-card shadow="hover" class="ops-card">
          <div slot="header"><span>监控与集群</span></div>
          <div class="ops-status">
            <el-tag type="success">Grafana 待接入</el-tag>
            <el-tag type="warning">Prometheus 待接入</el-tag>
            <el-tag>Kuboard 待接入</el-tag>
          </div>
          <p class="ops-tip">这里预留给监控大盘、指标系统和集群控制台，后续接入后可统一在本页聚合展示。</p>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :xs="24" :lg="24">
        <el-card shadow="hover" class="ops-card">
          <div slot="header"><span>运维建议</span></div>
          <ul class="ops-list">
            <li>优先从 OpenClaw 发起巡检与排障</li>
            <li>配置修改统一从 Nacos 进入</li>
            <li>发布与构建统一走 Jenkins</li>
            <li>接口联调统一从 Swagger 进入</li>
          </ul>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog
      title="OpenClaw AI 对话"
      :visible.sync="chatDialogVisible"
      width="88vw"
      top="4vh"
      append-to-body
      custom-class="openclaw-chat-dialog"
    >
      <div class="chat-frame-wrap">
        <iframe
          v-if="chatDialogVisible"
          class="chat-frame"
          :src="chatUrl"
          frameborder="0"
          allow="clipboard-read; clipboard-write"
        ></iframe>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button @click="chatDialogVisible = false">关 闭</el-button>
        <el-button type="primary" @click="openInNewTab">新标签打开</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'OpsCenter',
  data() {
    return {
      chatDialogVisible: false,
      chatUrl: '/ai/chat'
    }
  },
  methods: {
    openChatDialog() {
      this.chatDialogVisible = true
    },
    openInNewTab() {
      window.open(this.chatUrl, '_blank')
    }
  }
}
</script>

<style lang="scss" scoped>
.ops-card { margin-bottom: 20px; }
.ops-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.ops-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 26px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(63, 169, 255, 0.16);
  border: 1px solid rgba(63, 169, 255, 0.3);
  color: #8fd3ff;
  font-size: 12px;
  font-weight: 700;
}
.ai-card {
  min-height: 208px;
}
.ai-entry-title {
  font-size: 18px;
  font-weight: 700;
  color: #ffffff;
  margin-bottom: 10px;
}
.ai-entry-desc {
  color: rgba(234, 242, 255, 0.76);
  line-height: 1.85;
  margin: 0 0 18px;
}
.ai-actions {
  display: flex;
  align-items: center;
  gap: 14px;
}
.ops-links {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.ops-status {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 14px;
}
.ops-tip {
  color: rgba(234, 242, 255, 0.76);
  line-height: 1.8;
  margin: 0;
}
.ops-list {
  margin: 0;
  padding-left: 18px;
  color: rgba(234, 242, 255, 0.78);
  line-height: 1.9;
}
.chat-frame-wrap {
  height: 72vh;
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid rgba(255,255,255,0.08);
  background: rgba(255,255,255,0.03);
}
.chat-frame {
  width: 100%;
  height: 100%;
  background: #0f1728;
}
</style>

<style lang="scss">
.openclaw-chat-dialog .el-dialog__body {
  padding-top: 12px !important;
}
</style>
