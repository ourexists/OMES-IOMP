# WuKongIM

在线客服用独立的 WuKongIM。不要把它写进 `docker-compose.yml`，也不要共用 OMES 的 PostgreSQL、Redis、RabbitMQ。

管理端悬浮窗和工作台只调 Admin 的 `/im/**`（生产经 `/api/im/**`），由 Admin 代发；聊天记录留在 WuKongIM。  
生产 Web Nginx 另提供同源前缀 `/wkim/` → 宿主机 `127.0.0.1:5001`（去掉 `/wkim`），供前端直连 WuKongIM HTTP 时使用（`VITE_WKIM_BASE_URL=/wkim`）。不要把 `5001` 直接映射到公网，也不要打开 WebSocket 端口。

在本目录执行，项目名是 `omes-wukongim`，不进入 OMES 的 Compose：

```bash
docker compose up -d
```

镜像是 `registry.cn-shanghai.aliyuncs.com/wukongim/wukongim:v2`。宿主机只把 HTTP API 绑在 `127.0.0.1:5001`。TCP `5100`、WebSocket `5200`、管理台 `5300`、演示页 `5172` 不映射出来。

`wk.yaml` 的 `managerToken` 与 Admin 的 `omes.im.secret` 是同一个值。请求头名是 `token`。Admin 侧仍直连 `http://127.0.0.1:5001`，不要改成公网 `/wkim`。

1. 启动容器，确认 `http://127.0.0.1:5001/health` 可访问；经 Web 时确认 `http://<web>/wkim/health`。
2. 在 `config/admin.properties` 写入后重启 Admin：

```
omes.im.enabled=true
omes.im.api-url=http://127.0.0.1:5001
omes.im.secret=35ad3392afb0aa0dd50acb0faae13d042c4525f6f25d27caeb46b6d74355cc67
```

发送是否允许由 Admin 在代发前检查租户和账号状态。
