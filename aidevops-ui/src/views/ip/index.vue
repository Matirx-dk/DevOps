<template>
  <div class="app-container page-shell page-ip">
    <div class="search-card">
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
        <el-form-item label="IP地址" prop="ipAddress">
          <el-input v-model="queryParams.ipAddress" placeholder="请输入IP地址" clearable style="width: 200px" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="主机名" prop="hostName">
          <el-input v-model="queryParams.hostName" placeholder="请输入主机名" clearable style="width: 200px" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="环境" prop="env">
          <el-select v-model="queryParams.env" placeholder="全部" clearable style="width: 150px">
            <el-option label="测试环境" value="test" />
            <el-option label="生产环境" value="cloud" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 150px">
            <el-option label="正常" value="0" />
            <el-option label="禁用" value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="创建时间">
          <el-date-picker v-model="dateRange" style="width: 240px" value-format="yyyy-MM-dd" type="daterange" range-separator="-" start-placeholder="开始日期" end-placeholder="结束日期"></el-date-picker>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="toolbar-card">
      <el-row :gutter="10" class="mb8">
        <el-col :span="1.5">
          <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd">新增</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate">修改</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete">删除</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport">导出</el-button>
        </el-col>
        <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" :columns="columns"></right-toolbar>
      </el-row>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="ipList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column label="IP地址" align="center" key="ipAddress" prop="ipAddress" v-if="columns.ipAddress.visible" :show-overflow-tooltip="true" />
        <el-table-column label="主机名" align="center" key="hostName" prop="hostName" v-if="columns.hostName.visible" :show-overflow-tooltip="true" />
        <el-table-column label="端口" align="center" key="port" prop="port" v-if="columns.port.visible" width="80" />
        <el-table-column label="环境" align="center" key="env" prop="env" v-if="columns.env.visible" width="100">
          <template slot-scope="scope">
            <el-tag :type="scope.row.env === 'cloud' ? 'danger' : 'success'" size="small">
              {{ scope.row.env === 'cloud' ? '生产' : '测试' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="用途" align="center" key="purpose" prop="purpose" v-if="columns.purpose.visible" :show-overflow-tooltip="true" />
        <el-table-column label="标签" align="center" key="tags" prop="tags" v-if="columns.tags.visible" :show-overflow-tooltip="true">
          <template slot-scope="scope">
            <span v-if="!scope.row.tags">-</span>
            <el-tag v-else v-for="tag in scope.row.tags.split(',')" :key="tag" size="small" style="margin-right: 4px">{{ tag }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" align="center" key="status" prop="status" v-if="columns.status.visible" width="80">
          <template slot-scope="scope">
            <el-switch v-model="scope.row.status" active-value="0" inactive-value="1" @change="handleStatusChange(scope.row)" />
          </template>
        </el-table-column>
        <el-table-column label="备注" align="center" key="remark" prop="remark" v-if="columns.remark.visible" :show-overflow-tooltip="true" />
        <el-table-column label="创建时间" align="center" key="createTime" prop="createTime" v-if="columns.createTime.visible" width="180" />
        <el-table-column label="操作" align="center" width="160" fixed="right">
          <template slot-scope="scope">
            <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)">修改</el-button>
            <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
    </div>

    <!-- 新增/修改对话框 -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="IP地址" prop="ipAddress">
          <el-input v-model="form.ipAddress" placeholder="请输入IP地址" style="width: 100%" />
        </el-form-item>
        <el-form-item label="主机名" prop="hostName">
          <el-input v-model="form.hostName" placeholder="请输入主机名" style="width: 100%" />
        </el-form-item>
        <el-form-item label="端口" prop="port">
          <el-input-number v-model="form.port" :min="1" :max="65535" style="width: 100%" placeholder="端口号" />
        </el-form-item>
        <el-form-item label="环境" prop="env">
          <el-radio-group v-model="form.env">
            <el-radio label="test">测试环境</el-radio>
            <el-radio label="cloud">生产环境</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="用途" prop="purpose">
          <el-input v-model="form.purpose" placeholder="请输入用途" style="width: 100%" />
        </el-form-item>
        <el-form-item label="标签" prop="tags">
          <el-input v-model="form.tags" placeholder="多个标签用逗号分隔，如: k8s,mysql,redis" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" style="width: 100%" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitForm">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'IpManagement',
  data() {
    return {
      loading: false,
      ipList: [],
      total: 0,
      showSearch: true,
      single: true,
      multiple: true,
      dateRange: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        ipAddress: undefined,
        hostName: undefined,
        env: undefined,
        status: undefined
      },
      columns: [
        { key: 'ipAddress', label: 'IP地址', visible: true },
        { key: 'hostName', label: '主机名', visible: true },
        { key: 'port', label: '端口', visible: true },
        { key: 'env', label: '环境', visible: true },
        { key: 'purpose', label: '用途', visible: true },
        { key: 'tags', label: '标签', visible: true },
        { key: 'status', label: '状态', visible: true },
        { key: 'remark', label: '备注', visible: true },
        { key: 'createTime', label: '创建时间', visible: true }
      ],
      dialogTitle: '',
      dialogVisible: false,
      form: {
        ipAddress: '',
        hostName: '',
        port: 22,
        env: 'test',
        purpose: '',
        tags: '',
        remark: '',
        status: '0'
      },
      rules: {
        ipAddress: [
          { required: true, message: 'IP地址不能为空', trigger: 'blur' },
          {
            pattern: /^(25[0-5]|2[0-4]\d|[0-1]?\d?\d)(\.(25[0-5]|2[0-4]\d|[0-1]?\d?\d)){3}$/,
            message: '请输入正确的IP地址',
            trigger: 'blur'
          }
        ],
        hostName: [{ required: true, message: '主机名不能为空', trigger: 'blur' }],
        env: [{ required: true, message: '请选择环境', trigger: 'change' }]
      },
      ids: [],
      // 模拟数据（后端实现前先用这个展示）
      mockData: [
        { ipId: 1, ipAddress: '192.168.10.101', hostName: 'devops-1', port: 22, env: 'test', purpose: 'K8s 控制面 / Jenkins', tags: 'k8s,jenkins', status: '0', remark: '内网 FRP 映射 47.115.133.185:4101', createTime: '2026-03-28 10:00:00' },
        { ipId: 2, ipAddress: '192.168.10.102', hostName: 'devops-2', port: 22, env: 'test', purpose: 'K8s 工作节点', tags: 'k8s', status: '0', remark: '内网 FRP 映射 47.115.133.185:4102', createTime: '2026-03-28 10:01:00' },
        { ipId: 3, ipAddress: '192.168.10.103', hostName: 'devops-3', port: 22, env: 'test', purpose: 'K8s 工作节点', tags: 'k8s,sonarqube', status: '0', remark: '内网 FRP 映射 47.115.133.185:4103', createTime: '2026-03-28 10:02:00' },
        { ipId: 4, ipAddress: '192.168.10.104', hostName: 'devops-4', port: 22, env: 'test', purpose: 'K8s 工作节点', tags: 'k8s', status: '0', remark: '内网 FRP 映射 47.115.133.185:4104', createTime: '2026-03-28 10:03:00' },
        { ipId: 5, ipAddress: '47.115.133.185', hostName: 'beijing-proxy', port: 22, env: 'cloud', purpose: '公网入口 / Nginx / FRP Server', tags: 'frp,nginx,mihomo', status: '0', remark: '香港云服务器，代理节点', createTime: '2026-03-25 09:00:00' },
        { ipId: 6, ipAddress: '38.76.217.113', hostName: 'hk-server', port: 22, env: 'cloud', purpose: '香港云服务器', tags: 'proxy', status: '0', remark: '无密码 SSH', createTime: '2026-03-20 08:00:00' },
        { ipId: 7, ipAddress: '192.168.1.104', hostName: 'harbor', port: 80, env: 'cloud', purpose: 'Harbor 镜像仓库', tags: 'harbor', status: '0', remark: '内网地址，Nginx 反代公网访问', createTime: '2026-03-25 10:00:00' },
        { ipId: 8, ipAddress: '10.137.88.151', hostName: 'mihomo-proxy', port: 7890, env: 'test', purpose: '开发环境 HTTP 代理', tags: 'mihomo,proxy', status: '0', remark: 'devops-1 上的透明代理，frp 内网机器均配置此代理', createTime: '2026-04-01 12:00:00' }
      ]
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      // TODO: 后端实现后替换为真实接口
      // this.$axios.get('/system/ip', { params: this.queryParams }).then(res => { ... })
      setTimeout(() => {
        let data = this.mockData
        if (this.queryParams.ipAddress) {
          data = data.filter(d => d.ipAddress.includes(this.queryParams.ipAddress))
        }
        if (this.queryParams.hostName) {
          data = data.filter(d => d.hostName.includes(this.queryParams.hostName))
        }
        if (this.queryParams.env) {
          data = data.filter(d => d.env === this.queryParams.env)
        }
        if (this.queryParams.status) {
          data = data.filter(d => d.status === this.queryParams.status)
        }
        const start = (this.queryParams.pageNum - 1) * this.queryParams.pageSize
        const end = start + this.queryParams.pageSize
        this.ipList = data.slice(start, end)
        this.total = data.length
        this.loading = false
      }, 200)
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.dateRange = []
      this.$resetForm('queryForm')
      this.handleQuery()
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.ipId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.dialogTitle = '添加IP'
      this.dialogVisible = true
    },
    handleUpdate(row) {
      this.reset()
      const rowData = row.ipId ? row : this.ipList.find(d => d.ipId === this.ids[0])
      this.form = { ...rowData }
      this.dialogTitle = '修改IP'
      this.dialogVisible = true
    },
    handleDelete(row) {
      const ipIds = row.ipId ? [row.ipId] : this.ids
      this.$confirm('是否确认删除所选IP记录？', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        // TODO: 后端实现后替换为真实接口
        // this.$axios.delete('/system/ip/' + ipIds.join(',')).then(() => { this.getList() })
        this.$modal.msgSuccess('删除成功（Mock）')
        this.getList()
      }).catch(() => {})
    },
    handleStatusChange(row) {
      // TODO: 后端实现后替换为真实接口
      // this.$axios.put('/system/ip/' + row.ipId + '/status?status=' + row.status)
      this.$modal.msgSuccess('状态修改成功（Mock）')
    },
    handleExport() {
      this.download('system/ip/export', { ...this.queryParams }, `ip_management_${new Date().getTime()}.xlsx`)
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        // TODO: 后端实现后替换为真实接口
        // const action = this.form.ipId ? 'put' : 'post'
        // this.$axios[action]('/system/ip', this.form).then(() => { ... })
        this.$modal.msgSuccess((this.form.ipId ? '修改' : '新增') + '成功（Mock）')
        this.dialogVisible = false
        this.getList()
      })
    },
    reset() {
      this.form = {
        ipAddress: '',
        hostName: '',
        port: 22,
        env: 'test',
        purpose: '',
        tags: '',
        remark: '',
        status: '0'
      }
      this.$resetForm('form')
    }
  }
}
</script>

<style scoped>
.page-ip .el-input-number {
  width: 100%;
}
</style>
 
