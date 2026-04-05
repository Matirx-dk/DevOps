<template>
  <div class="workbench-page app-container">
    <el-row :gutter="20">
      <el-col :xs="24" :lg="16">
        <el-card shadow="hover" class="workbench-card hero-card">
          <div slot="header" class="card-header">
            <div>
              <div class="hero-mark">AIOps</div>
              <span class="hero-section-title">工作台</span>
            </div>
            <span class="header-tag">AIDevOps</span>
          </div>
          <div class="hero-panel">
            <div class="hero-main">
              <h2 class="hero-title">智能运维协同入口</h2>
              <p class="hero-desc">
                聚焦发布、质量、监控与运维协同，快速进入 AI 对话、配置中心、流水线、接口文档和运维中心，减少在多个系统之间切换。
              </p>
              <div class="hero-tags">
                <span>CI/CD</span>
                <span>Kubernetes</span>
                <span>Harbor</span>
                <span>Observability</span>
              </div>
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
          </div>
        </el-card>

        <el-card shadow="hover" class="workbench-card action-card">
          <div slot="header" class="card-header"><span>常用操作</span></div>
          <el-row :gutter="16">
            <el-col :xs="24" :md="12">
              <div class="action-item glass-action">
                <h3>开始巡检</h3>
                <p>优先检查网关、Nacos、Jenkins、入口链路和集群状态，必要时从 AI 对话发起排障。</p>
                <router-link to="/ai/chat"><el-button type="primary" size="small">打开 AI 对话</el-button></router-link>
              </div>
            </el-col>
            <el-col :xs="24" :md="12">
              <div class="action-item glass-action">
                <h3>进入运维中心</h3>
                <p>统一进入配置中心、流水线、接口文档，以及后续接入的监控和集群工具。</p>
                <router-link to="/ops"><el-button type="primary" size="small">打开运维中心</el-button></router-link>
              </div>
            </el-col>
          </el-row>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="8">
        <el-card shadow="hover" class="workbench-card action-card">
          <div slot="header" class="card-header"><span>快捷入口</span></div>
          <div class="quick-links">
            <router-link to="/ai/chat"><el-button type="text" size="small">AI 对话</el-button></router-link>
            <router-link to="/ops"><el-button type="text" size="small">运维中心</el-button></router-link>
            <router-link to="/quality"><el-button type="text" size="small">质量中心</el-button></router-link>
            <el-link href="https://devops.zoudekang.cloud/nacos/" target="_blank" type="text" size="small">Nacos 配置中心</el-link>
            <el-link href="https://devops.zoudekang.cloud/jenkins/" target="_blank" type="text" size="small">Jenkins 流水线</el-link>
            <el-link href="/swagger-ui/index.html" target="_blank" type="text" size="small">Swagger 接口文档</el-link>
          </div>
        </el-card>

        <el-card shadow="hover" class="workbench-card action-card">
          <div slot="header" class="card-header"><span>使用建议</span></div>
          <ul class="tips-list">
            <li>巡检和排障优先从 AI 对话进入。</li>
            <li>配置修改统一从 Nacos 进入。</li>
            <li>发布和构建统一从 Jenkins 进入。</li>
            <li>接口联调统一从 Swagger 进入。</li>
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
  border-radius: 18px;
  overflow: hidden;
}

.hero-card {
  background: linear-gradient(135deg, #0f1728 0%, #18243a 55%, #203456 100%);
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.18);
}

.action-card {
  background: rgba(255, 255, 255, 0.97);
  border: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #1a1a2e;
}

.hero-card .card-header {
  color: #eaf2ff;
}

.hero-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(63, 169, 255, 0.16);
  border: 1px solid rgba(63, 169, 255, 0.3);
  color: #8fd3ff;
  font-size: 12px;
  font-weight: 700;
  margin-bottom: 12px;
}

.hero-section-title {
  font-size: 18px;
  font-weight: 700;
  color: #eaf2ff;
  margin-left: 12px;
}

.header-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(63, 169, 255, 0.16);
  border: 1px solid rgba(63, 169, 255, 0.3);
  color: #8fd3ff;
  font-size: 12px;
  font-weight: 700;
}

.hero-panel {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.hero-title {
  margin: 0 0 12px;
  color: #ffffff;
  font-size: 26px;
  font-weight: 700;
}

.hero-desc {
  margin: 0;
  color: rgba(234, 242, 255, 0.78);
  line-height: 1.9;
}

.hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 20px;
}

.hero-tags span {
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #dce9ff;
  font-size: 13px;
}

.summary-row { margin-bottom: 4px; }

.summary-item {
  border-radius: 14px;
  padding: 16px 14px;
  color: #fff;
  margin-bottom: 12px;
  box-shadow: 0 10px 24px rgba(31, 45, 61, 0.18);
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

.action-item {
  padding: 18px;
  border-radius: 14px;
  background: #f8f9fa;
  border: 1px solid rgba(0, 0, 0, 0.06);
  margin-bottom: 12px;

  h3 {
    margin: 0 0 10px;
    color: #1a1a2e;
    font-size: 16px;
    font-weight: 700;
  }

  p {
    margin: 0 0 14px;
    color: #666;
    font-size: 13px;
    line-height: 1.7;
  }
}

.quick-links {
  display: flex;
  flex-direction: column;
  gap: 4px;

  a {
    text-decoration: none;
  }
}

.tips-list {
  margin: 0;
  padding-left: 18px;
  color: #555;
  font-size: 14px;
  line-height: 2;
}
</style>
