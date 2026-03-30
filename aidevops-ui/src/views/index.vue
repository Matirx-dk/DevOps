<template>
  <div class="ai-home app-container">
    <el-row :gutter="20">
      <el-col :xs="24" :lg="9">
        <el-card class="hero-card" shadow="hover">
          <div class="hero-head">
            <h1>AIDevOps AI智能运维系统</h1>
            <p>将 Kubernetes、Harbor、Jenkins、Nacos 与 OpenClaw 对话能力整合到统一运维入口。</p>
          </div>
          <div class="scene-list">
            <div class="scene-item">
              <h3>智能巡检</h3>
              <p>直接发起集群巡检、服务健康检查、入口链路验证与配置核对。</p>
            </div>
            <div class="scene-item">
              <h3>发布协同</h3>
              <p>围绕镜像构建、Jenkins 流水线、Harbor 推送与 Kubernetes 发布进行联动分析。</p>
            </div>
            <div class="scene-item">
              <h3>故障排查</h3>
              <p>快速定位 Pod、Ingress、NFS、证书、网关与配置中心等问题。</p>
            </div>
          </div>
          <div class="quick-links">
            <p><el-link href="https://devops.zoudekang.cloud/nacos/" target="_blank">Nacos 配置中心</el-link></p>
            <p><el-link href="https://devops.zoudekang.cloud/jenkins/" target="_blank">Jenkins 流水线</el-link></p>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="15">
        <el-card class="chat-card" shadow="hover">
          <div slot="header" class="chat-header">
            <span>OpenClaw 运维对话面板</span>
            <el-link href="/openclaw/" target="_blank" type="primary">新窗口打开</el-link>
          </div>
          <iframe class="openclaw-frame" src="/openclaw/" title="OpenClaw Control"></iframe>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
export default {
  name: 'AIDevOpsHome'
}
</script>

<style lang="scss" scoped>
.ai-home {
  .hero-card, .chat-card { margin-bottom: 20px; }
  .hero-head h1 { margin: 0 0 10px; color: #1f2d3d; }
  .hero-head p { color: #606266; line-height: 1.8; margin-bottom: 20px; }
  .scene-list {
    display: flex;
    flex-direction: column;
    gap: 16px;
    .scene-item {
      padding: 14px 16px;
      background: #f7f8fa;
      border-radius: 12px;
      h3 { margin: 0 0 8px; color: #303133; font-size: 16px; }
      p { margin: 0; color: #606266; line-height: 1.8; }
    }
  }
  .quick-links {
    margin-top: 20px;
    p { margin: 8px 0; }
  }
  .chat-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
  .openclaw-frame {
    width: 100%;
    height: 760px;
    border: 0;
    border-radius: 12px;
    background: #fff;
  }
}
</style>
