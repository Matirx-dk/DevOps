# AIDevOps K8s 一键部署说明

文件：`deploy/k8s/aidevops-all-in-one.yaml`

## 用法

```bash
kubectl apply -f deploy/k8s/aidevops-all-in-one.yaml
kubectl get pods -n aidevops-cloud -w
```

## 包含内容

- Namespace: `aidevops-cloud`
- ConfigMap:
  - `aidevops-sql-init`
  - `aidevops-gateway-local`
  - `aidevops-system-local`
- PV/PVC:
  - `aidevops-mysql-pv`
  - `aidevops-mysql-pvc`
- Job:
  - `aidevops-db-init`
- Deployment/Service:
  - `aidevops-mysql`
  - `aidevops-redis`
  - `aidevops-nacos`
  - `aidevops-auth`
  - `aidevops-system`
  - `aidevops-gateway`
  - `aidevops-ui`
- Ingress:
  - `devops1.zoudekang.cloud`
  - `nacos.zoudekang.cloud`

## 当前默认值

- MySQL root 密码：`password`
- Nacos 数据库：`ry-config`
- 业务数据库：`ry-cloud`
- UI 域名：`devops1.zoudekang.cloud`
- Nacos 域名：`nacos.zoudekang.cloud`
- 镜像：沿用当前集群正在跑的镜像地址/摘要

## 前置条件

1. 集群内已安装 `ingress-nginx`
2. 域名已解析到入口机
3. 对于**全新安装**，`192.168.1.104:/data/nfs/share/aidevops-mysql` 可被集群挂载
4. 如果是从旧环境平滑升级，且已有 MySQL PV 已绑定旧路径，则需要注意：现网可能仍在使用 `/data/nfs/share/ruoyi-mysql`，不能直接在原 PV 上改 `persistentVolumeSource`
5. 集群节点能拉取这些镜像：
   - `mysql:8.0`
   - `redis:7-alpine`
   - `nacos/nacos-server:v2.2.3`
   - `192.168.1.104/aidevops/aidevops-auth@sha256:2e88aaa875bc146e429da73b2a20c5004058efc0bcb2ca6f98668778ac062bf1`
   - `192.168.1.104/aidevops/aidevops-system@sha256:7a07142f0046c0bcfc9705057a03e2424ba90a0f2569d129144ef1ed87fe93f7`
   - `192.168.1.104/aidevops/aidevops-gateway@sha256:be23b226c6c854ada39bd7088a317b21f70a8109d7a5ddd33389b01c9be2ea43`
   - `192.168.1.104/aidevops/aidevops-ui:demo`

## 说明

这个版本是按当前线上实际运行情况整理出来的“可直接落地版”。
已经把 SQL 里的 `localhost` Redis/MySQL/Gateway 引用改成了集群 Service 地址，避免再次出现：

- `Unable to connect to Redis`
- 数据库连本地地址失败
- springdoc/gateway 指向 localhost

如果后续要做更规范的版本，建议下一步再拆成：

- `namespace.yaml`
- `configmap.yaml`
- `storage.yaml`
- `middleware.yaml`
- `apps.yaml`
- `ingress.yaml`
