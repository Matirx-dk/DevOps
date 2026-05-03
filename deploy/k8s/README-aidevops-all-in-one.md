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
3. 对于**全新安装**，`192.168.1.100:/data/nfs/share/aidevops-mysql` 可被集群挂载
4. 如果是从旧环境平滑升级，且已有 MySQL PV 已绑定旧路径，则需要注意：现网可能仍在使用 `/data/nfs/share/ruoyi-mysql`，不能直接在原 PV 上改 `persistentVolumeSource`
5. 集群节点能拉取这些镜像：
   - `192.168.1.100:3443/dockerhub-proxy/library/mysql:8.0`
   - `192.168.1.100:3443/dockerhub-proxy/library/redis:7-alpine`
   - `192.168.1.100:3443/dockerhub-proxy/nacos/nacos-server:v2.2.3`
   - `192.168.1.100:3443/aidevops/aidevops-auth:restored-auth`
   - `192.168.1.100:3443/aidevops/aidevops-system:restored-system`
   - `192.168.1.100:3443/aidevops/aidevops-gateway:release-gateway-20260330-124936`
   - `192.168.1.100:3443/aidevops/aidevops-ui:release-ui-20260330-155056`

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

## 2026-04-01 迁移记录

- 动态存储类：`nfs-client`
- NFS dynamic provisioner 已部署在命名空间：`nfs-provisioner`
- Jenkins 已从手工 PV/PVC（`jenkins-home-pv` / `jenkins-home-pvc`）迁移到动态存储 PVC：`jenkins-home-dynamic-pvc`，旧 Jenkins PV/PVC 已下线删除。
- SonarQube（`aidevops-test`）已从手工 PV/PVC（`sonarqube-test-pv` / `sonarqube-test-pvc`）迁移到动态 PVC：`sonarqube-dynamic-pvc`，旧 SonarQube PV/PVC 已下线删除。
- MySQL（`aidevops-cloud`）已从手工 PV/PVC 迁移到动态 PVC：`aidevops-mysql-dynamic-pvc`，旧 cloud MySQL PV/PVC 已下线删除。
- MySQL（`aidevops-test`）已从手工 PV/PVC 迁移到动态 PVC：`aidevops-mysql-dynamic-pvc`，旧 test MySQL PV/PVC 已下线删除。
- `aidevops-test-data-pv/pvc` 与 `aidevops-test-logs-pv/pvc` 已确认闲置并清理。
- Harbor 已从手工共享卷（`harbor-shared-pv` / `harbor-shared-pvc`）迁移到动态 PVC：`harbor-shared-dynamic-pvc`。
- Harbor 旧 NFS 数据未直接删除，已归档到：`/vol1/1000/Devops/backup/harbor-manual-backup-20260401-172058`
- 当前镜像入口已统一收敛到：`192.168.1.100:3443`
- 当前建议：后续新建持久化工作负载优先使用 `storageClassName: nfs-client`，避免继续手工写死 NFS 路径。
