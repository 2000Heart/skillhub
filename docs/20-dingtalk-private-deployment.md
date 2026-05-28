# 钉钉企业网页应用私有化部署与登录接入手册

## 1. 文档目的

本文档面向计划将开源 SkillHub 私有化部署，并改造为“钉钉企业网页应用（DingTalk 工作台打开）”登录为主路径的开发与运维同学。

本文重点覆盖：

- 钉钉身份接入的两条登录链路（钉钉内免登 + 浏览器备用）
- 需要在 SkillHub 中启用的关键后端/前端配置
- 钉钉开放平台侧应配置哪些回调/域名

本文假设你已经完成 SkillHub 私有化部署基线（Docker Compose / Kubernetes、Postgres/Redis、HTTPS 公网入口等）。

---

## 2. 总体登录链路（两条路径）

SkillHub 的认证实现遵循“统一 Session 建立逻辑 + IdentityBinding 绑定外部身份”的原则，因此两条链路最终都会写入 SkillHub 统一的 Web 会话（Spring Session）。

### 路径 A：钉钉内免登（推荐）

1. 用户在钉钉工作台内打开 SkillHub 网页应用（H5 微应用 / 企业内部应用）
2. 前端通过 `dingtalk-jsapi` 调用 `dd.requestAuthCode` 获取 `code`
3. 前端调用：
   - `POST /api/v1/auth/dingtalk/login`
   - Body：`{ "code": "...", "corpId": "..." }`
4. 后端换取用户信息，基于 `providerCode=dingtalk` + `subject=unionId` 自动创建/绑定 SkillHub 用户
5. 后端建立 SkillHub Session，重定向回原始页面（`returnTo`）

关键代码（供排查）：

- 后端登录入口：`server/skillhub-app/.../controller/DingTalkAuthController.java`
- 后端用户解析与绑定：`server/skillhub-auth/.../dingtalk/DingTalkUserResolver.java`
- 身份绑定复用：`server/skillhub-auth/.../identity/IdentityBindingService.java`

### 路径 B：浏览器备用（必须提供以保证可用性）

当用户不是在钉钉环境内访问（或无法拿到 `corpId`）时，登录页仍会提供“钉钉登录”按钮。

该按钮会跳转到 SkillHub 提供的浏览器 OAuth 入口：

- `GET /api/v1/auth/dingtalk/oauth/authorize`

并在 SkillHub 回调：

- `GET /api/v1/auth/dingtalk/oauth/callback`

完成登录后回跳到登录页携带的 `returnTo`。

你需要在 SkillHub 与钉钉开放平台侧同时放通对应回调域名/地址。

---

## 3. 钉钉开放平台侧配置清单

下面以“企业内部网页应用 / H5 微应用”为例描述（具体 UI 名称可能略有差异）。

### 3.1 应用类型与安全域名

在钉钉开放平台创建企业内部应用，至少需要完成以下配置：

- 授权回调/重定向域名（HTTPS）
- H5 安全域名（确保 SkillHub 域名在白名单中）
- 记录 `Client ID` / `Client Secret`
- **个人权限（委托）**：浏览器 OAuth 回调会调用「获取用户通讯录个人信息」，必须开通 **`Contact.User.Read`**（通讯录个人信息读权限）。未开通时回调会失败。

### 3.2 回调地址（浏览器备用链路）

SkillHub 的浏览器链路回调地址固定为：

- `https://<SKILLHUB_PUBLIC_BASE_URL>/api/v1/auth/dingtalk/oauth/callback`

确保该地址在钉钉侧配置为允许回调。

---

## 4. SkillHub 私有版配置（关键环境变量）

本节给出推荐的“最小可用配置”清单。实际部署请配合你们的机密管理策略（Secret / Vault / 环境变量注入）。

### 4.1 后端（server）

在后端容器环境变量中配置：

- 启用 DingTalk 身份提供方
  - `SKILLHUB_DINGTALK_ENABLED=true`
- 钉钉应用凭据
  - `SKILLHUB_DINGTALK_CLIENT_ID=...`
  - `SKILLHUB_DINGTALK_CLIENT_SECRET=...`
- 单企业默认 corpId（可选，但建议填写，减少前端参数依赖）
  - `SKILLHUB_DINGTALK_DEFAULT_CORP_ID=...`（勿使用 `SKILLHUB_DINGTALK_CORP_ID`，Spring 无法绑定）
- 超级管理员自动赋权（可选）
  - `SKILLHUB_DINGTALK_SUPER_ADMIN_UNION_IDS=unionId1,unionId2,...`
- 组织目录同步（可选，默认开启）
  - `SKILLHUB_DINGTALK_ORG_SYNC_STARTUP_ENABLED=true` — 首次启动且无成功基线时执行全量同步
  - `SKILLHUB_DINGTALK_ORG_SYNC_FULL_CRON=0 30 2 * * ?` — 每日定时全量同步（Spring cron）

> 注：SkillHub 内部身份主键使用 `unionId`（稳定 UID），请不要用可变字段作为 subject。

### 4.2 前端（web）

在前端容器环境变量中配置：

- 选择钉钉模式
  - `SKILLHUB_WEB_AUTH_MODE=dingtalk`
- 打开钉钉免登
  - `SKILLHUB_WEB_DINGTALK_AUTH_ENABLED=true`
- 钉钉前端 clientId（用于 `dd.requestAuthCode`）
  - `SKILLHUB_WEB_DINGTALK_CLIENT_ID=...`
- 自动尝试免登（可选，建议保留）
  - `SKILLHUB_WEB_DINGTALK_AUTO_LOGIN=true`

并确保：

- `SKILLHUB_PUBLIC_BASE_URL=https://<你的SkillHub域名>`

该值用于浏览器 OAuth 回调组装与重定向。

---

## 5. 身份绑定与权限治理建议

### 5.1 JIT 建档（无需独立维护员工信息）

用户首次登录将自动完成：

- 创建 SkillHub `user_account`
- 创建 `identity_binding(provider_code=dingtalk, subject=unionId)`
- 将用户加入命名空间（默认包含 global 的成员行为，取决于初始化逻辑）

因此不会再要求你维护“SkillHub 自己的员工信息库”（姓名/头像可在登录时同步）。

### 5.2 超级管理员初始化策略

如果你需要某些员工在首次登录后直接具备平台管理权限，可以配置：

- `SKILLHUB_DINGTALK_SUPER_ADMIN_UNION_IDS`

配置时请填 unionId 列表。

---

## 6. 联调验证清单（建议按顺序）

1. 钉钉内打开 SkillHub 网页应用
   - 观察登录是否能自动完成（路径 A）
   - 调用 `/api/v1/auth/me` 确认返回正确用户
2. 使用普通浏览器访问 SkillHub 登录页
   - 确认登录页显示“钉钉登录”
   - 完成浏览器备用链路（路径 B）
3. 验证权限开关
   - 被禁用户应无法建立有效 Session
   - 超级管理员登录后可访问管理页面
4. 验证 CLI publish/token
   - 走设备码授权后，确保 `/cli/auth` 能完成回跳登录

---

## 7. 常见问题与排查方向

### 7.1 浏览器回调 500 / 钉钉返回 Contact.User.Read

日志中若出现 `AccessTokenPermissionDenied` 或 `requiredScopes":["Contact.User.Read"]`：

1. 钉钉开放平台 → 应用 → **权限管理** → **个人权限** → 申请并开通 **`Contact.User.Read`**
2. 让用户重新走一遍「钉钉登录」授权（scope 已包含该权限）
3. 确认应用已发布/生效（部分企业需管理员审批权限）

### 7.2 浏览器回调失败（403/404）

优先检查：

- `SKILLHUB_PUBLIC_BASE_URL` 是否为真实 HTTPS 域名
- 钉钉开放平台是否已配置回调地址白名单
- 回调地址是否包含正确的路径 `/api/v1/auth/dingtalk/oauth/callback`

### 7.3 钉钉内 H5 远程调试（免登联调）

本地 `pnpm run dev` / `make dev-web` 时，Vite 会自动在页面 `<head>` 注入钉钉官方 H5 远程调试脚本：

```html
<script src="https://g.alicdn.com/code/npm/@ali/dingtalk-h5-remote-debug/0.1.3/index.js"></script>
```

使用步骤：

1. 在钉钉开放平台将微应用首页指向你的 HTTPS 入口（如 ngrok 域名）
2. 用钉钉打开该页面，按调试工具提示完成首次配对
3. 在 PC 端使用钉钉开发者工具查看 console / 网络请求，排查 `requestAuthCode` 与登录接口

该脚本**仅在开发服务器注入**，生产构建不会包含。

### 7.4 微应用首页 URL（免登）

**公网入口**与 OAuth 相同，例如 `https://<你的域名>`。

推荐将钉钉微应用 **应用首页** 配置为：

```text
https://<你的域名>/login?returnTo=/dashboard
```

说明：

- 免登逻辑在 **`/login` 页面**执行（`requestAuthCode`），打开根路径 `/` 只会看到营销首页，不会自动登录。
- 若 URL 未带 `corpid`，需在前端 runtime-config 配置 `dingtalkDefaultCorpId`（与后端 `SKILLHUB_DINGTALK_DEFAULT_CORP_ID` 一致）。
- 钉钉从工作台打开时通常会自动追加 `?corpid=...`，此时可省略配置。

### 7.5 钉钉免登失败（requestAuthCode 返回失败）

常见原因：

- 缺少 `corpId`（配置 `SKILLHUB_WEB_DINGTALK_DEFAULT_CORP_ID` 或确保工作台链接带参）
- `SKILLHUB_WEB_DINGTALK_CLIENT_ID` 配置错误
- 前端是否在正确的钉钉环境内运行

---

## 8. 组织目录同步（通讯录投影）

启用 `SKILLHUB_DINGTALK_ENABLED` 后，SkillHub 可将钉钉通讯录投影到本地表（部门、用户档案、部门关系、角色绑定），供管理端查询与权限初始化使用。

### 8.1 同步触发方式

| 方式 | 说明 |
|------|------|
| 启动时 | `SKILLHUB_DINGTALK_ORG_SYNC_STARTUP_ENABLED=true` 且尚无成功基线时自动全量同步 |
| 定时任务 | `SKILLHUB_DINGTALK_ORG_SYNC_FULL_CRON`（默认每天 02:30） |
| 管理 API | `POST /api/v1/admin/dingtalk/sync/full`（需 `USER_ADMIN` 或 `SUPER_ADMIN`） |
| 事件回调 | `POST /api/v1/dingtalk/events/callback`（钉钉推送组织变更时增量处理） |

### 8.2 管理端页面

前端路由（需平台用户管理权限）：

- `/admin/org-sync` — 同步状态与手动全量同步
- `/admin/org-structure` — 部门树浏览
- `/admin/org-users` — 组织用户列表

### 8.3 权限初始化

首次为尚未初始化平台角色的钉钉用户批量赋权：

- `POST /api/v1/admin/dingtalk/permissions/bootstrap?dryRun=false`

`dryRun=true` 时仅返回将影响的用户数量，不写入。

相关设计见 `docs/03-authentication-design.md`（DingTalk 小节）与 `docs/06-api-design.md`（Admin DingTalk API）。

---

## 9. 本地联调命令

```bash
# 可选：在 .dev/dingtalk.env 中导出凭据与 SKILLHUB_DEV_API_PORT
make dev-server-restart
make dingtalk-smoke

# 通过 ngrok 暴露 HTTPS 并写入 SKILLHUB_PUBLIC_BASE_URL
./scripts/ngrok-dingtalk-dev.sh
```

`make dev-web` / `pnpm run dev` 在开发模式下会为页面注入钉钉 H5 远程调试脚本，便于工作台内排查免登问题。

---

## 10. 文档补充

如果你还需要在钉钉网页应用中使用 JSAPI（选人/选会话等），可在后续迭代中继续接入，并沿用当前登录链路的 session 结果即可。

相关文档索引：

- [03-authentication-design.md](./03-authentication-design.md) — 认证架构与 DingTalk 身份映射
- [06-api-design.md](./06-api-design.md) — REST 契约
- [09-deployment.md](./09-deployment.md) — 发布环境变量
- [dev-workflow.md](./dev-workflow.md) — 本地开发流程

