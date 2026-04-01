# Jenkinsfile 逻辑说明（测试流水线）

## 目标

当前测试流水线目标：

1. 拉取 `test` 分支代码
2. 识别本次提交实际改动了哪些服务
3. 只编译/构建改动服务
4. 镜像统一推送到 Harbor 项目：`aidevops-test`
5. 镜像 tag 使用 **代码提交说明 + commit short sha**
6. 在测试命名空间 `aidevops-test` 中只更新改动服务
7. 增加基础代码安全/质量检测步骤
8. 整个流水线改为 **Kubernetes agent Pod 执行**，不再依赖 Jenkins controller 本体工具链

## 当前约定

- Harbor 地址：`harbor.zoudekang.cloud`
- 测试镜像项目：`aidevops-test`
- 测试命名空间：`aidevops-test`
- Jenkins agent namespace：`jenkins`
- agent ServiceAccount：`jenkins-agent`
- Harbor registry secret：`harbor-regcred`
- agent Pod 使用公共基础镜像：
  - `docker.io/library/maven:3.9.9-eclipse-temurin-17`
  - `docker.io/library/node:18-alpine`
  - `gcr.io/kaniko-project/executor:v1.23.2-debug`

## agent 执行模型

Jenkinsfile 使用 Kubernetes 动态 agent Pod，Pod 内包含两个主要容器：

### 1. maven
用于：

- `git checkout`
- Maven 编译
- 后端基础安全检测
- 动态下载 `kubectl` 后执行发布

### 2. node
用于：

- Node/NPM 前端构建
- 前端 `npm audit`

### 3. kaniko
用于：

- 镜像构建
- 镜像推送到 Harbor

这样做的好处是：

- controller 不需要安装 `docker / kubectl / mvn / npm`
- 更适合 Kubernetes 环境
- 使用 Kaniko 避免依赖宿主机 docker socket

## tag 规则

使用最新一次 Git 提交说明（commit subject）生成 tag：

- 原始提交说明：`fix login bug`
- 处理后：`fix-login-bug-<shortsha>`

示例：

- `fix-login-bug-a1b2c3d`

处理规则：

- 转小写
- 非字母数字字符替换为 `-`
- 连续 `-` 折叠
- 尾部拼接短 commit id

## 变更识别逻辑

Jenkinsfile 会根据 `git diff` 识别改动：

- `aidevops-auth/` -> 构建 `auth`
- `aidevops-gateway/` -> 构建 `gateway`
- `aidevops-modules/aidevops-system/` -> 构建 `system`
- `aidevops-ui/` -> 构建 `ui`

如果改动涉及公共模块，则会触发所有后端服务重新构建：

- `pom.xml`
- `aidevops-common/`
- `aidevops-api/`
- `aidevops-modules/pom.xml`
- `aidevops-common/pom.xml`
- `docker/build/`
- `Jenkinsfile`
- `deploy/k8s/`

## 构建逻辑

### 后端

统一使用：

- `docker/build/backend.Dockerfile`

只对有改动的服务构建镜像：

- `aidevops-auth`
- `aidevops-gateway`
- `aidevops-system`

构建参数：

- `JAR_PATH=aidevops-auth/target/aidevops-auth.jar`
- `JAR_PATH=aidevops-gateway/target/aidevops-gateway.jar`
- `JAR_PATH=aidevops-modules/aidevops-system/target/aidevops-modules-system.jar`

### 前端

统一使用：

- `docker/build/frontend.Dockerfile`

只在 `aidevops-ui/` 有改动时构建 `aidevops-ui`

## 推送策略

测试环境镜像统一推送到：

- `harbor.zoudekang.cloud/aidevops-test/aidevops-auth:<tag>`
- `harbor.zoudekang.cloud/aidevops-test/aidevops-gateway:<tag>`
- `harbor.zoudekang.cloud/aidevops-test/aidevops-system:<tag>`
- `harbor.zoudekang.cloud/aidevops-test/aidevops-ui:<tag>`

## 部署策略

Jenkins 只更新有改动的 Deployment：

- `deployment/aidevops-auth`
- `deployment/aidevops-gateway`
- `deployment/aidevops-system`
- `deployment/aidevops-ui`

更新方式为：

- `kubectl set image`
- `kubectl rollout status`

## 代码安全/质量检测

当前 Jenkinsfile 已补入基础检测步骤：

### 后端
- 当前联调阶段临时跳过 OWASP Dependency Check，优先验证构建、推镜像、部署主链路

### 前端
- 执行 `npm audit --audit-level=high`（不阻断）

说明：

当前实现先采用 **不阻塞式基础检测**，适合先把流程跑通。

如果后续要提高要求，可以再升级为：

1. 接入 SonarQube
2. 增加 Trivy / Grype 镜像扫描
3. 将高危漏洞改为阻断发布

## 配套文件

- Repo root: `Jenkinsfile`
- `docker/build/jenkins-agent-tools.Dockerfile`
- `deploy/k8s/jenkins-agent-rbac.yaml`
- `deploy/k8s/jenkins-harbor-regcred.yaml.example`
- `deploy/k8s/README-jenkins-agent.md`

## 联调前检查项

1. Jenkins 已安装 Kubernetes plugin
2. Jenkins 已配置 Kubernetes Cloud
3. `harbor.zoudekang.cloud/aidevops/jenkins-agent-tools:20260401` 已构建并推送
4. `jenkins-agent-rbac.yaml` 已应用
5. `jenkins` namespace 中已创建 `harbor-regcred`
6. Jenkins pipeline job 仍然从仓库 `test` 分支读取根目录 `Jenkinsfile`
