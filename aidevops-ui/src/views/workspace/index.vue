<template>
  <div class="workspace-page app-container">
    <el-row :gutter="20">
      <el-col :xs="24" :lg="15">
        <el-card shadow="hover" class="workspace-card hero-card">
          <div class="hero-title">AIDevOps 工作台</div>
          <div class="hero-desc">
            面向日常巡检、发布协同、配置治理与问题处理的统一入口。这里不重复首页内容，重点放运维常用入口和实际操作建议。
          </div>
          <el-row :gutter="14" class="summary-row">
            <el-col :xs="12" :md="6">
              <div class="summary-item blue">
                <div class="summary-name">AI 对话</div>
                <div class="summary-value">OpenClaw</div>
              </div>
            </el-col>
            <el-col :xs="12" :md="6">
              <div class="summary-item green">
                <div class="summary-name">配置治理</div>
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
                <div class="summary-name">接口调试</div>
                <div class="summary-value">Swagger</div>
              </div>
            </el-col>
          </el-row>
        </el-card>

        <el-card shadow="hover" class="workspace-card">
          <div slot="header"><span>推荐操作</span></div>
          <el-row :gutter="16">
            <el-col :xs="24" :md="12">
              <div class="action-card">
                <h3>开始巡检</h3>
                <p>优先检查集群状态、网关路由、Nacos 配置中心、Jenkins 流水线和入口链路是否正常。</p>
                <el-link href="https://devops.zoudekang.cloud/openclaw/" target="_blank" type="primary">打开 AI 对话</el-link>
              </div>
            </el-col>
            <el-col :xs="24" :md="12">
              <div class="action-card">
                <h3>进入运维中心</h3>
                <p>统一进入配置中心、流水线、接口文档，以及后续接入的监控与质量工具。</p>
                <router-link to="/ops">
                  <el-link type="primary">打开运维中心</el-link>
                </router-link>
              </div>
            </el-col>
          </el-row>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="9">
        <el-card shadow="hover" class="workspace-card">
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

        <el-card shadow="hover" class="workspace-card">
          <div slot="header"><span>工作台说明</span></div>
          <ul class="tips-list">
            <li>首页保持原样，不在这里重复首页内容。</li>
            <li>工作台主要承担运维协同、快捷入口和常用建议。</li>
            <li>外部平台入口统一收口到运维中心与质量中心。</li>
            <li>需要巡检或排障时，优先从 AI 对话进入。</li>
          </ul>
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
.workspace-card {
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
