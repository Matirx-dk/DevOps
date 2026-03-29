# ruoyi-cloud Helm Chart

## 安装

```bash
helm install ruoyi-cloud ./deploy/helm/ruoyi-cloud -n ruoyi-cloud --create-namespace
```

## 卸载

```bash
helm uninstall ruoyi-cloud -n ruoyi-cloud
```

## 关键配置

请至少覆盖这些 values：

- `auth.image`
- `system.image`
- `gateway.image`
- `ui.image`
- `ingress.uiHost`
- `ingress.nacosHost`
- `mysql.rootPassword`
- `mysql.nfs.server`
- `mysql.nfs.path`

建议通过单独 values 文件安装：

```bash
helm install ruoyi-cloud ./deploy/helm/ruoyi-cloud \
  -n ruoyi-cloud --create-namespace \
  -f my-values.yaml
```

仓库里也附带了一个当前集群可直接参考的示例：

- `deploy/helm/ruoyi-cloud/values.current-cluster.yaml`

## SQL 初始化

默认：
- `sqlInit.enabled=false`

### 方式一：使用 Chart 自带 SQL（推荐）

开启：

```yaml
sqlInit:
  enabled: true
  mode: bundled
```

Chart 已内置：
- `files/ry_config.sql`
- `files/ry_cloud.sql`
- `files/quartz.sql`

这些文件已经按 Kubernetes 场景做过一轮基础替换：
- Redis 不走 localhost
- MySQL 不走 localhost
- Gateway URL 不走 localhost
- Nacos 菜单地址改为域名入口

### 方式二：使用外部 inline SQL

```yaml
sqlInit:
  enabled: true
  mode: inline
  inline:
    ryConfigSql: |
      ...
    ryCloudSql: |
      ...
    quartzSql: |
      ...
```

## 已保留的关键修复

- `ruoyi-auth` 使用 `SPRING_DATA_REDIS_HOST/PORT`
- Redis 使用集群 Service 地址
- MySQL 使用集群 Service 地址
- Gateway/System 使用 ConfigMap 本地覆盖
- Nacos 入口默认使用 `/nacos/`
