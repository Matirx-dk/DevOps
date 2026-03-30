# AIDevOps Cloud Helm Chart

## 安装

```bash
helm install aidevops-cloud ./deploy/helm/ruoyi-cloud -n aidevops-cloud --create-namespace
```

## 卸载

```bash
helm uninstall aidevops-cloud -n aidevops-cloud
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
helm install aidevops-cloud ./deploy/helm/ruoyi-cloud \
  -n aidevops-cloud --create-namespace \
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

当前默认已优先把基础镜像切到 Harbor 代理缓存地址：

- `192.168.1.104/dockerhub-proxy/library/busybox:1.36`
- `192.168.1.104/dockerhub-proxy/library/mysql:8.0`
- `192.168.1.104/dockerhub-proxy/library/redis:7-alpine`
- `192.168.1.104/dockerhub-proxy/nacos/nacos-server:v2.2.3`

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

## Harbor Proxy Cache 使用说明

当前环境已在 `192.168.1.104` 的 Harbor 上配置：

- Registry Endpoint: `dockerhub`
- Proxy Cache Project: `dockerhub-proxy`

### 为什么要这样做

这样可以让集群优先通过 Harbor 拉取 Docker 官方镜像，避免直接访问 Docker Hub 不稳定或过慢。

### 关键规则

Docker Hub 官方镜像在 Harbor proxy cache 中，通常要带 `library/` 路径。

例如：

- 正确：`192.168.1.104/dockerhub-proxy/library/mysql:8.0`
- 正确：`192.168.1.104/dockerhub-proxy/library/redis:7-alpine`
- 错误：`192.168.1.104/dockerhub-proxy/mysql:8.0`

Nacos 这类非 library 路径镜像则保持原组织名：

- `192.168.1.104/dockerhub-proxy/nacos/nacos-server:v2.2.3`

### 首次拉取行为

第一次拉取某个镜像时，Harbor 会去 Docker Hub 回源并缓存，所以首次可能会慢一些。
后续节点再次拉取同一镜像时，会直接命中 Harbor 缓存。

### 示例

```bash
ctr -n k8s.io images pull 192.168.1.104/dockerhub-proxy/library/mysql:8.0
ctr -n k8s.io images pull 192.168.1.104/dockerhub-proxy/library/redis:7-alpine
ctr -n k8s.io images pull 192.168.1.104/dockerhub-proxy/nacos/nacos-server:v2.2.3
```

### Chart 当前默认策略

Helm Chart 中已优先把这些基础镜像改为 Harbor proxy/cache 地址：

- busybox
- mysql
- redis
- nacos

业务镜像仍然保持使用你自己的 Harbor 项目：

- `192.168.1.104/aidevops/...`

## 已保留的关键修复

- `aidevops-auth` 使用 `SPRING_DATA_REDIS_HOST/PORT`
- Redis 使用集群 Service 地址
- MySQL 使用集群 Service 地址
- Gateway/System 使用 ConfigMap 本地覆盖
- Nacos 入口默认使用 `/nacos/`
