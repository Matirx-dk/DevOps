<template>
  <div class="ops-center app-container">
    <el-row :gutter="20">
      <el-col :xs="24" :lg="8">
        <el-card shadow="hover" class="ops-card">
          <div slot="header"><span>配置与发布</span></div>
          <div class="ops-links">
            <el-link href="https://devops.zoudekang.cloud/nacos/" target="_blank">Nacos 配置中心</el-link>
            <el-link href="https://devops.zoudekang.cloud/jenkins/" target="_blank">Jenkins 流水线</el-link>
            <el-link href="/swagger-ui/index.html" target="_blank">Swagger 接口文档</el-link>
            <el-link href="https://devops.zoudekang.cloud/openclaw/" target="_blank">AI 对话入口</el-link>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="8">
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
      <el-col :xs="24" :lg="8">
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
.ops-card { margin-bottom: 20px; }
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
  color: #606266;
  line-height: 1.8;
  margin: 0;
}
.ops-list {
  margin: 0;
  padding-left: 18px;
  color: #606266;
  line-height: 1.9;
}
</style>
