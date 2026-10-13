# omes-iomp-web-admin

基于 Vue、TypeScript、Vite 和 Ant Design Vue 的管理端，包含设备、点检、物料、生产计划、用户权限和系统管理页面。公共后端启动与部署入口见 [项目总览](../readme.md)。

浏览器访问本模块的静态站点，认证和业务 API 通过 SAS 网关（默认 9400）进入后端。开发服务器默认端口 5173。

## 开发与构建

Node.js 版本需满足 [package-lock.json](package-lock.json) 中的依赖 engines；当前锁文件包含 `^22.18.0 || >=24.11.0` 要求。

在本目录执行：

```bash
npm ci
npm run dev
```

| 命令 | 用途 |
| --- | --- |
| `npm run dev` | 启动 Vite 开发服务器 |
| `npm run build` | vue-tsc 类型检查并构建 `dist/` |
| `npm run build:dist` | 仅运行 Vite 构建，不检查类型 |
| `npm run preview` | 本地预览已构建产物 |

## 清理

构建产物在 `dist/`，Vite 缓存在 `node_modules/.vite`，vue-tsc 增量信息在 `node_modules/.tmp`。依赖异常时再删除整个 `node_modules`，然后重新执行 `npm ci`。

```powershell
Remove-Item -Recurse -Force dist, node_modules\.vite, node_modules\.tmp -ErrorAction SilentlyContinue
```

```bash
rm -rf dist node_modules/.vite node_modules/.tmp
```

开发前启动 Admin 和 SAS；[vite.config.ts](vite.config.ts) 中的 `GATEWAY_PROXY_PREFIXES` 定义相对路径请求的代理范围。

## 环境与运行时配置

开发配置见 [.env.development](.env.development)，生产配置见 [.env.production](.env.production)。本机覆盖值可写入 `.env.development.local` 或 `.env.production.local`。

| 变量 | 用途 |
| --- | --- |
| `VITE_APP_TITLE` | 页面标题 |
| `VITE_GATEWAY_PORT` | 网关端口兜底值，默认 9400 |
| `VITE_SAS_BASE_URL` | SAS 网关地址；同源代理部署可使用空基址 |
| `VITE_BAIDU_MAP_AK` | 百度地图浏览器端 AK |
| `VITE_ENABLE_REMOTE_I18N` | 是否加载远程国际化 properties |

`VITE_*` 在构建时注入，修改后需重新构建。应用启动还会通过 `/open/frontend-config` 加载网关和地图运行时配置，合并逻辑见 [frontend-config.ts](src/config/frontend-config.ts) 和 [gateway.ts](src/config/gateway.ts)。

## 部署

将 `dist/` 交给静态服务器托管。使用仓库 Docker 镜像时，先完成构建，再使用 [Dockerfile](../docker/web/Dockerfile)；同源反向代理及 SPA 回退规则见 [nginx.conf](../docker/web/nginx.conf)。统一构建和部署命令见 [docker 部署模块](../docker/README.md)。

独立静态站点可配置网关绝对地址，跨域时需同时配置 SAS 允许的来源。同源部署需代理认证、业务和静态资源路径，不能只配置首页回退。

`/static/**`、地图、组态等资源需随实际使用功能配置服务来源，避免页面可打开但资源请求失败。

## 认证与权限

Token 存储键为 `mes-token`，OAuth2 客户端为 `mes`，验证码授权类型为 `captcha`。请求携带 `Authorization`、`x-era-platform` 和 `x-route-tenant`；平台标识为 `mes-edge`。

未登录时跳转 `/login`；菜单通过 `/permission/currentAccPermissionTree` 加载。新增页面时核对 [view-map.ts](src/router/view-map.ts) 的组件映射和 [vue-spa-paths.ts](src/config/vue-spa-paths.ts) 的页面路径声明。

## 代码入口

| 目录 | 用途 |
| --- | --- |
| `src/api/`、`src/types/` | 业务接口与类型 |
| `src/views/` | 按业务域划分的页面 |
| `src/components/`、`src/composables/` | 通用组件与组合式逻辑 |
| `src/config/` | 网关、认证、品牌及 API 配置 |
| `src/router/`、`src/stores/` | 路由、权限与应用状态 |
| `src/i18n/`、`src/locales/` | 国际化加载与内置文案 |
