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
              <router-link to="/ai-chat"><el-button type="primary" size="small">打开对话框</el-button></router-link>
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
            <router-link to="/ai-chat"><el-link>AI 对话入口</el-link></router-link>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="7">
        <el-card shadow="hover" class="ops-card">
          <div slot="header"><span>监控与集群</span></div>
          <div class="ops-links">
            <el-link href="https://test.zoudekang.cloud/grafana/" target="_blank">K8s 集群监控</el-link>
          </div>
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
  </div>
</template>

<script>
export default {
  name: 'OpsCenter'
}
</script>

<style lang="scss" scoped>
.ops-card {
  margin-bottom: 20px;
  border-radius: 16px;
}
.ops-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #1a1a2e;
}
.ops-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 26px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(63, 169, 255, 0.12);
  border: 1px solid rgba(63, 169, 255, 0.25);
  color: #3fa9ff;
  font-size: 12px;
  font-weight: 700;
}
.ai-card {
  min-height: 180px;
}
.ai-entry-title {
  font-size: 18px;
  font-weight: 700;
  color: #1a1a2e;
  margin-bottom: 10px;
}
.ai-entry-desc {
  color: #666;
  font-size: 14px;
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
  gap: 14px;
}
.ops-status {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 14px;
}
.ops-tip {
  color: #888;
  font-size: 13px;
  line-height: 1.8;
  margin: 0;
}
.ops-list {
  margin: 0;
  padding-left: 20px;
  color: #555;
  font-size: 14px;
  line-height: 2;
}
</style>
