# AIDevOps AI智能运维系统

AIDevOps 是一个基于 Vue + Spring Cloud Alibaba + Kubernetes 的 AI 智能运维系统，目标是将集群巡检、故障排查、镜像构建、流水线发布、配置中心管理与运维入口统一到一个平台中。

## 核心定位

- 面向 Kubernetes 的智能运维控制台
- 面向 Harbor / Jenkins / Nacos 的统一运维入口
- 支持 AI 对话式操作与日常巡检辅助
- 支持镜像构建、发布部署、集群检查和入口治理

## 主要模块

- `aidevops-ui`：前端控制台
- `aidevops-gateway`：系统网关
- `aidevops-auth`：认证服务
- `aidevops-api`：接口定义
- `aidevops-common`：公共组件
- `aidevops-modules`：业务模块
- `deploy/k8s`：Kubernetes 清单
- `deploy/helm`：Helm 部署模板
- `docker`：镜像构建与本地容器部署文件
- `deploy/nginx/devops.zoudekang.cloud.conf`：香港公网入口机当前使用的单域名路径转发配置参考

## 当前访问入口

- 控制台：`https://devops.zoudekang.cloud/`
- Nacos：`https://devops.zoudekang.cloud/nacos/`
- Jenkins：`https://devops.zoudekang.cloud/jenkins/`

## 当前仓库用途

这个仓库当前同时承担两类用途：

1. AIDevOps 系统源码
2. 与集群现网相关的 K8s / Helm / Docker 发布文件

后续建议继续把：

- AI 对话能力
- 运维分析面板
- Jenkins Pipeline
- Harbor 镜像构建流程

统一收敛成完整的智能运维平台发布链路。
ci retest Sat Apr  4 23:58:29 UTC 2026
# test
# test2
