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

## 当前约定

- Harbor 地址：`harbor.zoudekang.cloud`
- 测试镜像项目：`aidevops-test`
- 测试命名空间：`aidevops-test`
- Jenkins 凭据：
  - Harbor 凭据 ID：`harbor-admin`

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

## 构建逻辑

### 后端

统一使用：

- `docker/build/backend.Dockerfile`

只对有改动的服务构建镜像：

- `aidevops-auth`
- `aidevops-gateway`
- `aidevops-system`

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
- 尝试执行 OWASP Dependency Check（若 Jenkins 节点可用）

### 前端
- 执行 `npm audit --audit-level=high`

说明：

当前实现先采用 **不阻塞式基础检测**（失败不直接中断流水线），适合先跑通流程。

如果后续要提高要求，可以再升级为：

1. 接入 SonarQube
2. 增加 Trivy / Grype 镜像扫描
3. 将高危漏洞改为阻断发布

## 当前文件

- Repo root: `Jenkinsfile`

## 后续建议

1. 在 Jenkins 中创建/确认 Harbor 凭据：`harbor-admin`
2. 确认 Jenkins 节点具备：
   - `docker`
   - `kubectl`
   - `mvn`
   - `npm`
3. 如果要启用 SonarQube 质量门禁，可在后续版本里加入：
   - `withSonarQubeEnv(...)`
   - `waitForQualityGate()`
