# Harbor 迁移预案（草稿）

## 当前结论

当前 Harbor 已恢复正常，但**暂不建议继续把 `harbor-shared-pvc` 从手工共享卷迁到动态 PVC**。

当前实际共享卷：

- PVC: `harbor-shared-pvc`
- PV: `harbor-shared-pv`
- NFS: `192.168.1.100:/vol1/1000/Devops/harbor`

当前共享卷内主要子目录：

- `database`
- `registry`
- `redis`
- `jobservice`
- `trivy`

## 为什么暂不建议立刻迁

1. Harbor 是基础设施核心组件，影响面比 Jenkins / SonarQube / MySQL 大。
2. 本次已经验证 Harbor 对目录权限/属主非常敏感，尤其是 `database` 子目录。
3. Harbor 当前是多组件共用一个共享卷，不适合像单一应用那样直接整体搬迁。
4. 一旦迁移失败，会直接影响：
   - 镜像拉取
   - 业务发布
   - proxy cache
   - 后续 CI/CD 链路

## 建议的迁移目标

后续若要迁 Harbor，建议目标不是“直接把现在这一整块共享卷换成一个动态 PVC”，而是**先评估拆分**：

### 方案 A：继续单共享卷，但迁到动态 PVC

优点：
- 改动相对少
- 与当前 Harbor chart 结构更接近

缺点：
- 风险集中
- 一旦新卷目录/权限异常，整个 Harbor 都受影响
- 回滚需要整体回退

### 方案 B：拆分多个 PVC（更推荐）

建议拆分思路：
- `harbor-database`
- `harbor-registry`
- `harbor-redis`
- `harbor-jobservice`
- `harbor-trivy`

优点：
- 结构更清晰
- 权限问题更容易隔离
- 后续扩容/备份更方便

缺点：
- 要改 Helm values / 模板
- 迁移设计更复杂
- 首次实施成本更高

## 推荐迁移前提

在真正动 Harbor 前，建议先满足这些条件：

1. 预留维护窗口
   - 建议有明确停机窗口
   - 避免业务发布高峰时操作

2. 先做完整目录备份/快照
   - 至少备份 `/vol1/1000/Devops/harbor`
   - 特别是 `database` 与 `registry`

3. 固化当前 Helm values
   - 导出当前 `helm get values -n harbor harbor -a`
   - 作为回滚基线

4. 固化当前运行状态
   - `kubectl -n harbor get all`
   - `kubectl -n harbor get pvc,pv`
   - `kubectl -n harbor get ingress`
   - `kubectl -n harbor get secret,configmap`

5. 明确回滚策略
   - 回滚到旧 PV/PVC
   - 回滚到旧 values
   - 回滚后验证 UI / API / push / pull / token

## 推荐执行顺序（未来实施时）

### 第一步：做静态盘点

- 确认 Harbor 当前版本
- 确认 chart values
- 确认各组件卷挂载关系
- 确认真实 NFS 路径与权限

### 第二步：做迁移演练（非生产）

优先在测试环境做一套 Harbor 演练：
- 新卷
- 新 values
- 新 PVC
- 验证 pull / push / proxy cache / 登录

### 第三步：正式迁移

建议顺序：
1. 停 Harbor 写入流量
2. 停 Harbor 工作负载
3. 拷贝旧数据到新卷
4. 修正目录权限/属主
5. 应用新 PVC / 新 values
6. 启动 Harbor
7. 验证

### 第四步：迁移后验证

至少验证这些：

1. Harbor UI 可登录
2. `api/v2.0/projects` 可访问
3. 公共项目拉取正常
4. 业务项目拉取正常
5. push 正常
6. proxy cache 首次回源正常
7. 节点侧 containerd / kubelet 拉镜像正常
8. Jenkins / 业务发布链路正常

## 本轮已经踩过的风险点（后续必须规避）

1. 不要把 Harbor 实际 NFS 路径和别的共享目录混淆
   - 正确路径：`/vol1/1000/Devops/harbor`

2. 不要假设 Harbor database 目录权限可以在 Pod 内轻松修复
   - NFS 服务端属主/权限才是根本

3. 不要在 Harbor 控制面异常时继续改匿名访问、token、project 等配置
   - 先保证 `harbor-database` 正常

4. 不要把 Harbor 和普通业务应用按同样方式粗暴迁卷
   - Harbor 需要单独方案

## 当前建议

### 当前阶段
- 保留 `harbor-shared-pvc`
- 不在本轮继续迁 Harbor

### 下一步更适合做的事
- 继续把普通业务/测试环境卷动态化
- 单独准备 Harbor 迁移实施方案
- 条件成熟后再安排 Harbor 专项迁移
