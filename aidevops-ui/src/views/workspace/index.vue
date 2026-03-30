<template>
  <div class="workspace-page app-container">
    <el-row :gutter="20">
      <el-col :xs="24" :lg="14">
        <el-card shadow="hover" class="workspace-card">
          <div slot="header"><span>工作台</span></div>
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
        <el-card class="workspace-card" shadow="hover">
          <div slot="header"><span>快捷入口</span></div>
          <p><el-link href="https://devops.zoudekang.cloud/openclaw/" target="_blank">OpenClaw 对话</el-link></p>
          <p><el-link href="https://devops.zoudekang.cloud/nacos/" target="_blank">Nacos 配置中心</el-link></p>
          <p><el-link href="https://devops.zoudekang.cloud/jenkins/" target="_blank">Jenkins 流水线</el-link></p>
          <p><el-link href="/swagger-ui/index.html" target="_blank">Swagger 接口文档</el-link></p>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
export default {
  name: 'WorkspaceIndex'
}
</script>

<style lang="scss" scoped>
.workspace-card { margin-bottom: 20px; }
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
</style>
