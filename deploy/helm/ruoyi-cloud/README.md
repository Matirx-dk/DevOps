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

## SQL 初始化

默认：
- `sqlInit.enabled=false`

如果你要用 Chart 直接初始化数据库，需要在 values 中填入：
- `sqlInit.ryConfigSql`
- `sqlInit.ryCloudSql`
- `sqlInit.quartzSql`

## 已保留的关键修复

- `ruoyi-auth` 使用 `SPRING_DATA_REDIS_HOST/PORT`
- Redis 使用集群 Service 地址
- MySQL 使用集群 Service 地址
- Gateway/System 使用 ConfigMap 本地覆盖
- Nacos 入口默认使用 `/nacos/`
