# Jenkins Kubernetes Agent 落地说明

本目录用于把 Jenkins 测试流水线从 controller 本体执行，切换为 Kubernetes 动态 agent Pod 执行。

## 目标

- Jenkins controller 仅负责调度
- 编译、检测、构建、推送、部署都在 agent Pod 中完成
- 镜像构建使用 Kaniko，不依赖宿主机 docker socket
- 发布目标命名空间：`aidevops-test`

## 本次新增文件

- `docker/build/jenkins-agent-tools.Dockerfile`
  - 保留为备选自定义 agent 工具镜像
  - 当前 Jenkinsfile 已切换为直接使用公共基础镜像，不再强依赖这份镜像
- `deploy/k8s/jenkins-agent-rbac.yaml`
  - agent Pod 使用的 `ServiceAccount`
  - 对 `aidevops-test` 的 Deployment 更新权限
- `deploy/k8s/jenkins-harbor-regcred.yaml.example`
  - Harbor registry secret 示例
- `Jenkinsfile`
  - 已改为使用 Kubernetes agent + Kaniko

## 先决条件

### 1. Jenkins 安装插件

至少确认已安装：

- Kubernetes
- Pipeline
- Git
- Credentials Binding

### 2. Jenkins 配置 Kubernetes Cloud

在 Jenkins 系统配置中添加 Kubernetes Cloud，通常建议：

- Kubernetes URL：集群内默认即可（若 Jenkins 本身就在集群内）
- Jenkins URL：`http://jenkins.jenkins.svc.cluster.local:8080`
- Jenkins tunnel：`jenkins.jenkins.svc.cluster.local:50000`
- 命名空间：`jenkins`

### 3. 当前 agent 基础镜像来源

当前 Jenkinsfile 直接使用公共镜像：

- `docker.io/library/maven:3.9.9-eclipse-temurin-17`
- `docker.io/library/node:18-alpine`
- `gcr.io/kaniko-project/executor:v1.23.2-debug`

这样可以绕开 Harbor 代理仓库的基础镜像认证问题。

### 4. 应用 agent RBAC

```bash
kubectl apply -f deploy/k8s/jenkins-agent-rbac.yaml
```

### 5. 创建 Harbor 拉取/推送 secret

推荐直接在集群创建：

```bash
kubectl -n jenkins create secret docker-registry harbor-regcred \
  --docker-server=192.168.1.100:3443 \
  --docker-username='<user>' \
  --docker-password='<password>' \
  --docker-email='devops@example.com'
```

## Jenkinsfile 当前执行逻辑

当前流水线在动态 agent Pod 中运行，Pod 内含两个主要容器：

- `builder`
  - 负责编译、测试、安全检查、kubectl 发布
- `kaniko`
  - 负责构建和推送镜像

## 镜像推送目标

统一推送到：

- `192.168.1.100:3443/aidevops-test/aidevops-auth:<tag>`
- `192.168.1.100:3443/aidevops-test/aidevops-gateway:<tag>`
- `192.168.1.100:3443/aidevops-test/aidevops-system:<tag>`
- `192.168.1.100:3443/aidevops-test/aidevops-ui:<tag>`

## 发布权限说明

当前 RBAC 只给了 `aidevops-test` 的 Deployment 更新能力，属于最小够用范围。

## 当前建议的联调顺序

1. 先构建并推送 `jenkins-agent-tools` 镜像
2. 给 Jenkins 配 Kubernetes Cloud
3. 应用 `jenkins-agent-rbac.yaml`
4. 创建 `harbor-regcred`
5. 触发 `aidevops-test-pipeline`
6. 先验证单服务改动是否能完成：
   - checkout
   - maven/node build
   - kaniko push
   - kubectl rollout

## 备注

- 当前安全检测先使用基础不阻断模式：
  - 后端：OWASP dependency-check（可用则执行）
  - 前端：`npm audit --audit-level=high`
- 如果后续要加强，可以继续接入：
  - SonarQube 质量门禁
  - Trivy 镜像扫描
  - 高危漏洞阻断发布
