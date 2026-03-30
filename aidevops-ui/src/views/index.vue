<template>
  <div class="ai-home app-container">
    <el-row :gutter="20">
      <el-col :xs="24" :lg="14">
        <el-card class="hero-card" shadow="hover">
          <div class="hero-head">
            <h1>AIDevOps AI智能运维系统</h1>
            <p>面向集群、容器、镜像仓库、发布流水线与日常巡检的统一智能运维工作台。</p>
          </div>
          <div class="chat-window">
            <div class="msg assistant">
              <div class="role">AI 助手</div>
              <div class="bubble">你好，我可以帮你分析集群状态、排查告警、生成发布方案、输出巡检结论。</div>
            </div>
            <div class="msg user">
              <div class="role">你</div>
              <div class="bubble">帮我检查 Kubernetes 集群、Harbor、Jenkins 和 NFS 当前状态。</div>
            </div>
            <div class="msg assistant">
              <div class="role">AI 助手</div>
              <div class="bubble">已生成巡检任务清单：集群健康、镜像仓库、流水线、存储、入口与证书链路。你可以从左侧菜单进入各模块继续处理。</div>
            </div>
          </div>
          <div class="chat-input">
            <el-input placeholder="输入运维指令，例如：发布新版本、分析告警、检查节点磁盘..." disabled>
              <el-button slot="append" type="primary">发送</el-button>
            </el-input>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="10">
        <el-card class="panel-card" shadow="hover">
          <div slot="header"><span>平台能力</span></div>
          <el-tag>AI 对话运维</el-tag>
          <el-tag type="success">Kubernetes</el-tag>
          <el-tag type="warning">Jenkins</el-tag>
          <el-tag type="info">Harbor</el-tag>
          <el-tag type="danger">Nacos</el-tag>
          <ul class="feature-list">
            <li>发布流水线自动化</li>
            <li>镜像构建与仓库治理</li>
            <li>集群健康检查与故障定位</li>
            <li>NFS / 证书 / Ingress 运维协同</li>
            <li>告警摘要与巡检报告</li>
          </ul>
        </el-card>
        <el-card class="panel-card" shadow="hover">
          <div slot="header"><span>推荐入口</span></div>
          <p><el-link href="https://devops.zoudekang.cloud/" target="_blank">业务控制台</el-link></p>
          <p><el-link href="https://devops.zoudekang.cloud/nacos/" target="_blank">Nacos 配置中心</el-link></p>
          <p><el-link href="https://devops.zoudekang.cloud/jenkins/" target="_blank">Jenkins 流水线</el-link></p>
          <p><el-link href="https://devops.zoudekang.cloud/openclaw/" target="_blank">OpenClaw 对话</el-link></p>
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
  .hero-card, .panel-card { margin-bottom: 20px; }
  .hero-head h1 { margin: 0 0 10px; color: #1f2d3d; }
  .hero-head p { color: #606266; line-height: 1.8; }
  .chat-window {
    background: #f7f8fa; border-radius: 12px; padding: 18px; margin: 20px 0;
    .msg { margin-bottom: 14px; }
    .role { font-size: 12px; color: #909399; margin-bottom: 6px; }
    .bubble {
      display: inline-block; max-width: 100%; padding: 12px 14px; border-radius: 12px;
      line-height: 1.7; background: #fff; color: #303133; box-shadow: 0 2px 8px rgba(0,0,0,.05);
    }
    .assistant .bubble { border-left: 4px solid #409EFF; }
    .user .bubble { border-left: 4px solid #67C23A; }
  }
  .feature-list { margin: 15px 0 0; padding-left: 18px; line-height: 2; color: #606266; }
}
</style>
