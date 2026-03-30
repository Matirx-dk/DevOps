# AIDevOps K8s 通用版说明

文件：`deploy/k8s/aidevops-all-in-one.generic.yaml`

这是从当前线上可运行版本抽出来的**通用模板版**，目的是方便迁移到别的 Kubernetes 集群。

## 和当前落地版的区别

这个 generic 版本去掉了以下强绑定：

- 私有镜像地址
- 固定域名
- 固定 NFS 服务地址
- 固定 MySQL 密码
- 当前线上镜像 digest

## 你需要替换的占位符

- `CHANGE_ME_REGISTRY/aidevops-auth:CHANGE_ME_TAG`
- `CHANGE_ME_REGISTRY/aidevops-system:CHANGE_ME_TAG`
- `CHANGE_ME_REGISTRY/aidevops-gateway:CHANGE_ME_TAG`
- `CHANGE_ME_REGISTRY/aidevops-ui:CHANGE_ME_TAG`
- `CHANGE_ME_DOMAIN_UI`
- `CHANGE_ME_DOMAIN_NACOS`
- `CHANGE_ME_NFS_SERVER`
- `CHANGE_ME_NFS_PATH`
- `CHANGE_ME_MYSQL_ROOT_PASSWORD`

## 还需要你补的内容

`ConfigMap/aidevops-sql-init` 里我保留成了模板注释：

- `01-ry-config.sql`
- `02-ry-cloud.sql`
- `03-quartz.sql`

也就是说，**generic 版结构可直接复用，但 SQL 初始化内容需要你按目标环境填进去**。

## 为什么这样处理

因为真正的“通用”不能继续把你当前环境里的这些信息硬编码进去：

- `192.168.1.104`
- `devops1.zoudekang.cloud`
- `nacos.zoudekang.cloud`
- 你当前 Harbor/Registry 地址
- 现网专用镜像摘要

## 仍然保留的关键修复思路

这个模板版仍然保留了已经验证过的关键配置：

- `aidevops-auth` 强制带 `SPRING_DATA_REDIS_HOST/PORT`
- Redis 统一走 `aidevops-redis` Service
- MySQL 统一走 `aidevops-mysql` Service
- Gateway / System 本地覆盖配置走 ConfigMap
- Nacos Web 入口默认按 `/nacos/` 处理

## 建议下一步

如果你后面要真正跨环境复用，建议继续拆成：

- `base/`
- `overlays/dev/`
- `overlays/prod/`

也就是做成 **kustomize** 或 **helm chart**，这样更适合长期维护。
