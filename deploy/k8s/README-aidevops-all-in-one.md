# AIDevOps All-in-One K8s 部署

> 仅供本地开发/单节点快速部署参考。生产环境请使用 `deploy/helm/`。

## 部署顺序

1. `namespace.yaml` — 创建 namespace
2. `nfs-subdir-provisioner.yaml` — NFS 存储供应器
3. `jenkins.yaml` — Jenkins master（需提前拉取镜像）
4. `jenkins-agent-rbac.yaml` — Agent RBAC
5. `jenkins-cache-pvc.yaml` — Maven/npm 缓存 PVC
6. `aidevops-all-in-one.yaml` — 全部微服务（需提前推送镜像）

## 前提条件

- NFS Server：`192.168.1.100`，path：`/vol1/1000/Devops`
- Ingress：节点 80/443 端口已开放
