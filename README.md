# AIDevOps 智能运维平台

基于 Vue + Spring Cloud Alibaba + Kubernetes 的 AI 智能运维系统，统一管理集群巡检、故障排查、镜像构建、流水线发布与配置中心。

## 技术栈

- **K8s** v1.28 + Jenkins + Harbor + SonarQube + Nacos
- **微服务**：auth / gateway / system
- **前端**：Vue + Element Plus
- **CI/CD**：Kaniko + Helm

## 目录结构

```
aidevops-ui/          前端
aidevops-auth/        认证服务
aidevops-gateway/     API网关
aidevops-modules/     业务模块（system/file/gen/job）
aidevops-common/      公共依赖
aidevops-api/         接口定义
deploy/
  helm/aidevops-cloud/   Helm部署模板
  k8s/                    K8s原生清单（参考）
docker/
  build/                 多阶段构建Dockerfile
  aidevops/              各模块独立Dockerfile
sql/                     数据库初始化脚本
```

## CI/CD 流程

- **test 分支**：推送到 test 环境，自动构建部署到 `aidevops-test`
- **main 分支**：合并后推送到 cloud 环境，自动构建部署到 `aidevops-cloud`
- 变更检测：仅构建实际改动的服务（auth/gateway/system/ui）
- SonarQube：仅分析变更的 .java 文件

## 部署

```bash
# Cloud 环境
helm upgrade --install aidevops-cloud deploy/helm/aidevops-cloud/ \
  -n aidevops-cloud --create-namespace \
  -f deploy/helm/aidevops-cloud/values.current-cluster.yaml
```
