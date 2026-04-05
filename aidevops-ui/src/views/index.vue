<template>
  <div class="workbench-page app-container">
    <el-row :gutter="16" class="stats-row">
      <el-col :xs="12" :sm="6">
        <div class="stat-card blue">
          <div class="stat-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/></svg>
          </div>
          <div class="stat-info">
            <div class="stat-value">4</div>
            <div class="stat-label">K8s 节点</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card green">
          <div class="stat-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2"/><path d="M3 9h18M9 21V9"/></svg>
          </div>
          <div class="stat-info">
            <div class="stat-value">8</div>
            <div class="stat-label">微服务</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card orange">
          <div class="stat-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 8v4l3 3"/></svg>
          </div>
          <div class="stat-info">
            <div class="stat-value">Nacos</div>
            <div class="stat-label">配置中心</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card purple">
          <div class="stat-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="3" width="20" height="14" rx="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/></svg>
          </div>
          <div class="stat-info">
            <div class="stat-value">Harbor</div>
            <div class="stat-label">镜像仓库</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="14">
        <!-- 一键操作 -->
        <el-card shadow="hover" class="section-card">
          <div slot="header" class="section-header">
            <span>快捷入口</span>
          </div>
          <div class="action-grid">
            <div class="action-btn" @click="$router.push('/ai-chat')">
              <div class="action-icon ai">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
              </div>
              <span>AI 对话</span>
            </div>
            <div class="action-btn" @click="$router.push('/ops')">
              <div class="action-icon ops">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M19.07 4.93l-1.41 1.41M5.34 18.66l-1.41 1.41M22 12h-2M4 12H2M19.07 19.07l-1.41-1.41M5.34 5.34L3.93 3.93M12 22v-2M12 4V2"/></svg>
              </div>
              <span>运维中心</span>
            </div>
            <a class="action-btn" href="https://devops.zoudekang.cloud/jenkins/" target="_blank">
              <div class="action-icon jenkins">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg>
              </div>
              <span>Jenkins</span>
            </a>
            <a class="action-btn" href="https://devops.zoudekang.cloud/nacos/" target="_blank">
              <div class="action-icon nacos">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 8v4l3 3"/></svg>
              </div>
              <span>Nacos</span>
            </a>
            <div class="action-btn" @click="$router.push('/quality')">
              <div class="action-icon quality">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
              </div>
              <span>SonarQube</span>
            </div>
            <a class="action-btn" href="/swagger-ui/" target="_blank">
              <div class="action-icon swagger">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 8 8 8"/></svg>
              </div>
              <span>Swagger</span>
            </a>
          </div>
        </el-card>

        <!-- 外部工具入口 -->
        <el-card shadow="hover" class="section-card">
          <div slot="header" class="section-header">
            <span>外部工具</span>
          </div>
          <div class="tool-list">
            <a class="tool-item" href="https://devops.zoudekang.cloud/harbor/" target="_blank">
              <div class="tool-icon harbor">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/></svg>
              </div>
              <div class="tool-info">
                <strong>Harbor 镜像仓库</strong>
                <span>容器镜像存储与分发</span>
              </div>
              <div class="tool-arrow">→</div>
            </a>
            <a class="tool-item" href="https://devops.zoudekang.cloud/grafana/" target="_blank">
              <div class="tool-icon grafana">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="18 20 18 12 22 6 18 2"/><polyline points="6 20 6 14 2 8 6 2"/></svg>
              </div>
              <div class="tool-info">
                <strong>Grafana 监控大盘</strong>
                <span>指标可视化与告警</span>
              </div>
              <div class="tool-arrow">→</div>
            </a>
            <a class="tool-item" href="https://devops.zoudekang.cloud/sonarqube/" target="_blank">
              <div class="tool-icon sonarqube">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
              </div>
              <div class="tool-info">
                <strong>SonarQube 代码质量</strong>
                <span>代码质量检测与漏洞扫描</span>
              </div>
              <div class="tool-arrow">→</div>
            </a>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="10">
        <!-- 节点信息 -->
        <el-card shadow="hover" class="section-card">
          <div slot="header" class="section-header">
            <span>测试环境节点</span>
          </div>
          <div class="node-table">
            <div class="node-header">
              <span>主机名</span>
              <span>角色</span>
              <span>IP</span>
            </div>
            <div class="node-row">
              <span>devops-1</span>
              <span>控制平面</span>
              <span>192.168.1.101</span>
            </div>
            <div class="node-row">
              <span>devops-2</span>
              <span>计算节点</span>
              <span>192.168.1.102</span>
            </div>
            <div class="node-row">
              <span>devops-3</span>
              <span>计算节点</span>
              <span>192.168.1.103</span>
            </div>
            <div class="node-row">
              <span>devops-4</span>
              <span>计算节点</span>
              <span>192.168.1.104</span>
            </div>
          </div>
        </el-card>

        <!-- 提示 -->
        <el-card shadow="hover" class="section-card">
          <div slot="header" class="section-header">
            <span>使用建议</span>
          </div>
          <ul class="tips-list">
            <li>发布构建统一走 Jenkins 流水线</li>
            <li>配置变更统一在 Nacos 中管理</li>
            <li>AI 对话可发起运维巡检与故障排查</li>
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
.workbench-page {
  padding: 20px 20px 0;
}

.stats-row {
  margin-bottom: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 14px;
  border-radius: 12px;
  margin-bottom: 12px;
  cursor: default;
  &:hover { transform: translateY(-1px); }
}

.stat-icon {
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  svg { width: 100%; height: 100%; }
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  line-height: 1;
  margin-bottom: 4px;
  color: #fff;
}

.stat-label {
  font-size: 12px;
  opacity: 0.85;
  color: #fff;
}

.stat-card.blue { background: linear-gradient(135deg, #3fa9ff, #215cff); }
.stat-card.green { background: linear-gradient(135deg, #34d399, #059669); }
.stat-card.orange { background: linear-gradient(135deg, #f59e0b, #d97706); }
.stat-card.purple { background: linear-gradient(135deg, #a78bfa, #7c3aed); }

.section-card {
  margin-bottom: 16px;
  border-radius: 12px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 15px;
  font-weight: 700;
  color: #111827;
  -webkit-text-fill-color: #111827;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.action-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 14px 8px;
  border-radius: 10px;
  background: #f8f9fa;
  border: 1px solid rgba(0, 0, 0, 0.06);
  cursor: pointer;
  text-decoration: none;
  color: #1a1a2e;
  font-size: 12px;
  font-weight: 600;
  transition: all 0.2s;
  &:hover {
    background: #eef1f6;
    transform: translateY(-2px);
  }
}

.action-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  svg { width: 18px; height: 18px; }
  &.ai { background: rgba(63, 169, 255, 0.12); color: #3fa9ff; }
  &.ops { background: rgba(52, 211, 153, 0.12); color: #34d399; }
  &.jenkins { background: rgba(245, 158, 11, 0.12); color: #f59e0b; }
  &.nacos { background: rgba(167, 139, 250, 0.12); color: #7c3aed; }
  &.quality { background: rgba(16, 185, 129, 0.12); color: #059669; }
  &.swagger { background: rgba(99, 102, 95, 0.12); color: #636f57; }
}

.tool-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.tool-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  background: #f8f9fa;
  text-decoration: none;
  transition: all 0.2s;
  &:hover { background: #eef1f6; transform: translateX(2px); }
}

.tool-icon {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  svg { width: 16px; height: 16px; }
  &.harbor { background: rgba(63, 169, 255, 0.12); color: #3fa9ff; }
  &.grafana { background: rgba(245, 158, 11, 0.12); color: #f59e0b; }
  &.sonarqube { background: rgba(52, 211, 153, 0.12); color: #059669; }
}

.tool-info {
  flex: 1;
  strong { font-size: 13px; color: #1a1a2e; font-weight: 600; }
  span { font-size: 11px; color: #999; }
}

.tool-arrow { color: #ccc; font-size: 14px; }

.node-table {
  border: 1px solid rgba(0,0,0,0.08);
  border-radius: 8px;
  overflow: hidden;
}

.node-header {
  display: grid;
  grid-template-columns: 1.5fr 1fr 1.5fr;
  background: #f8f9fa;
  padding: 8px 12px;
  font-size: 12px;
  font-weight: 600;
  color: #888;
  border-bottom: 1px solid rgba(0,0,0,0.06);
}

.node-row {
  display: grid;
  grid-template-columns: 1.5fr 1fr 1.5fr;
  padding: 8px 12px;
  font-size: 13px;
  color: #666;
  border-bottom: 1px solid rgba(0,0,0,0.04);
  &:last-child { border-bottom: none; }
}

.tips-list {
  margin: 0;
  padding-left: 16px;
  color: #555;
  font-size: 13px;
  line-height: 2.2;
}
</style>

<style lang="scss">
.workbench-page .el-card__header {
  padding: 12px 16px !important;
  border-bottom: 1px solid rgba(0,0,0,0.06) !important;
}
.workbench-page .el-card__body {
  padding: 14px 16px !important;
}
</style>
</parameter>
</script>
</minimax:tool_call>