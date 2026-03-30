<template>
  <div class="dashboard-container app-container">
    <el-row :gutter="20">
      <el-col :xs="24" :lg="16">
        <el-card shadow="hover">
          <div slot="header"><span>监控中心</span></div>
          <panel-group />
          <line-chart style="margin-top: 24px;" />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="8">
        <el-card shadow="hover" style="margin-bottom: 20px;">
          <div slot="header"><span>运行概览</span></div>
          <pie-chart />
        </el-card>
        <el-card shadow="hover">
          <div slot="header"><span>告警趋势</span></div>
          <bar-chart />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import PanelGroup from './PanelGroup'
import LineChart from './LineChart'
import PieChart from './PieChart'
import BarChart from './BarChart'

export default {
  name: 'DashboardIndex',
  components: {
    PanelGroup,
    LineChart,
    PieChart,
    BarChart
  }
}
</script>
