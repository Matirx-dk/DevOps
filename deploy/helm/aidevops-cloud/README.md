# AIDevOps Cloud Helm Chart

## 安装

```bash
helm install aidevops-cloud ./deploy/helm/aidevops-cloud -n aidevops-cloud --create-namespace
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
helm install aidevops-cloud ./deploy/helm/aidevops-cloud \
  -n aidevops-cloud --create-namespace \
  -f my-values.yaml
```

仓库里也附带了一个当前集群可直接参考的示例：

- `deploy/helm/aidevops-cloud/values.current-cluster.yaml`

## 当前集群注意事项（2026-03-30）

### 1. MySQL PV 路径兼容说明

当前线上 `aidevops-cloud` 已经完成应用层命名切换，但现网已存在的 MySQL PV 仍绑定旧 NFS 路径：

- 现网实际生效路径：`/data/nfs/share/ruoyi-mysql`

原因是 Kubernetes `PersistentVolume.spec.persistentVolumeSource` 不可变。
如果直接把已创建 PV 的 NFS 路径从旧值改成：

- `/data/nfs/share/aidevops-mysql`

Helm 升级会失败。

因此：
- **新装环境**可以直接使用 `aidevops-mysql`
- **升级现网环境**如果已经存在旧 PV，需继续沿用旧路径，或单独做一次数据迁移后再重建 PV

### 2. 当前公网入口现状

截至 2026-03-30 本次发布完成后，集群内 Helm 升级与 Pod rollout 已成功，但公网域名入口现状与 Helm 默认值不完全一致：

- `nacos.zoudekang.cloud`：可正常访问
- `devops.zoudekang.cloud/jenkins/`：可到 Jenkins
- `devops.zoudekang.cloud/`：当前返回 404，需要继续检查公网 Nginx 到集群 UI 的转发
- `devops.zoudekang.cloud/nacos/`：当前返回 404，需要继续检查单域名路径代理配置
- `devops1.zoudekang.cloud`：当前公网侧解析未就绪

所以如果你是按当前线上环境维护，建议把 **集群发布成功** 和 **公网入口已完全对齐** 视为两个独立步骤处理。

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
