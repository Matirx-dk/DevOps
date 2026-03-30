<template>
  <div class="workbench-page app-container">
    <el-row :gutter="20">
      <el-col :xs="24" :lg="16">
        <el-card shadow="hover" class="workbench-card hero-card">
          <div class="hero-title">AIDevOps 工作台</div>
          <div class="hero-desc">
            统一承载 AI 运维协同、配置治理、流水线入口、接口联调与平台状态概览，减少在多个系统之间来回切换。
          </div>

          <el-row :gutter="14" class="summary-row">
            <el-col :xs="12" :md="6">
              <div class="summary-item blue">
                <div class="summary-name">AI 运维</div>
                <div class="summary-value">OpenClaw</div>
              </div>
            </el-col>
            <el-col :xs="12" :md="6">
              <div class="summary-item green">
                <div class="summary-name">配置中心</div>
                <div class="summary-value">Nacos</div>
              </div>
            </el-col>
            <el-col :xs="12" :md="6">
              <div class="summary-item orange">
                <div class="summary-name">流水线</div>
                <div class="summary-value">Jenkins</div>
              </div>
            </el-col>
            <el-col :xs="12" :md="6">
              <div class="summary-item purple">
                <div class="summary-name">质量与接口</div>
                <div class="summary-value">Sonar / API</div>
              </div>
            </el-col>
          </el-row>
        </el-card>

        <el-card shadow="hover" class="workbench-card">
          <div slot="header"><span>推荐操作</span></div>
          <el-row :gutter="16">
            <el-col :xs="24" :md="12">
              <div class="action-card">
                <h3>开始巡检</h3>
                <p>优先检查集群、网关、Nacos、Jenkins 与入口链路状态，再决定是否进入运维中心继续处理。</p>
                <el-link href="https://devops.zoudekang.cloud/openclaw/" target="_blank" type="primary">打开 AI 对话</el-link>
              </div>
            </el-col>
            <el-col :xs="24" :md="12">
              <div class="action-card">
                <h3>进入运维中心</h3>
                <p>统一进入配置中心、流水线、接口文档，以及后续接入的监控、质量和集群管理能力。</p>
                <router-link to="/ops">
                  <el-link type="primary">打开运维中心</el-link>
                </router-link>
              </div>
            </el-col>
          </el-row>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="8">
        <el-card shadow="hover" class="workbench-card">
          <div slot="header"><span>快捷入口</span></div>
          <div class="quick-links">
            <p><el-link href="https://devops.zoudekang.cloud/openclaw/" target="_blank">AI 对话</el-link></p>
            <p><router-link to="/ops"><el-link>运维中心</el-link></router-link></p>
            <p><router-link to="/quality"><el-link>质量中心</el-link></router-link></p>
            <p><el-link href="https://devops.zoudekang.cloud/nacos/" target="_blank">Nacos 配置中心</el-link></p>
            <p><el-link href="https://devops.zoudekang.cloud/jenkins/" target="_blank">Jenkins 流水线</el-link></p>
            <p><el-link href="/swagger-ui/index.html" target="_blank">Swagger 接口文档</el-link></p>
          </div>
        </el-card>

        <el-card shadow="hover" class="workbench-card">
          <div slot="header"><span>工作台说明</span></div>
          <ul class="tips-list">
            <li>这里作为日常运维和平台协同的统一入口。</li>
            <li>AI 对话负责巡检、排障、发布辅助与问题归因。</li>
            <li>运维中心统一汇总配置、流水线、接口和后续监控入口。</li>
            <li>质量中心用于承接 SonarQube 等代码质量能力。</li>
          </ul>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
export default {
  name: 'WorkbenchIndex'
}
</script>

<style lang="scss" scoped>
.workbench-card {
  margin-bottom: 20px;
}

.hero-title {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 12px;
}

.hero-desc {
  color: #606266;
  line-height: 1.9;
  margin-bottom: 18px;
}

.summary-row {
  margin-top: 10px;
}

.summary-item {
  border-radius: 12px;
  padding: 16px 14px;
  color: #fff;
  margin-bottom: 12px;
}

.summary-name {
  font-size: 13px;
  opacity: 0.92;
  margin-bottom: 8px;
}

.summary-value {
  font-size: 18px;
  font-weight: 700;
}

.summary-item.blue { background: linear-gradient(135deg, #409EFF, #36CFC9); }
.summary-item.green { background: linear-gradient(135deg, #67C23A, #95DE64); }
.summary-item.orange { background: linear-gradient(135deg, #FA8C16, #FFC53D); }
.summary-item.purple { background: linear-gradient(135deg, #722ED1, #B37FEB); }

.action-card {
  height: 100%;
  padding: 18px;
  border-radius: 12px;
  background: #f7f8fa;
}

.action-card h3 {
  margin: 0 0 10px;
  color: #303133;
}

.action-card p {
  margin: 0 0 14px;
  color: #606266;
  line-height: 1.8;
}

.quick-links p {
  margin: 10px 0;
}

.tips-list {
  margin: 0;
  padding-left: 18px;
  color: #606266;
  line-height: 1.9;
}
</style>
