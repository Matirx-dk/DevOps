<template>
  <div class="quality-center app-container">
    <el-row :gutter="20">
      <el-col :xs="24" :lg="16">
        <el-card shadow="hover">
          <div slot="header"><span>质量中心</span></div>
          <p class="quality-desc">面向代码质量、漏洞治理与质量门禁的统一入口。当前优先整合 SonarQube，后续可继续补充项目级质量趋势与扫描结果展示。</p>
          <div class="quality-tags">
            <el-tag type="success">质量门禁</el-tag>
            <el-tag type="warning">漏洞扫描</el-tag>
            <el-tag type="info">代码异味</el-tag>
            <el-tag>技术债务</el-tag>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="8">
        <el-card shadow="hover">
          <div slot="header"><span>外部入口</span></div>
          <div class="quality-links">
            <el-link disabled>SonarQube 待接入</el-link>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
export default {
  name: 'QualityCenter'
}
</script>

<style lang="scss" scoped>
.quality-desc {
  color: #606266;
  line-height: 1.9;
  margin: 0 0 18px;
}
.quality-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.quality-links {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>
