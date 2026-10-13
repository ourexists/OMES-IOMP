# Docker 部署模块

集中维护本仓库已有的 Docker 构建与部署文件，不参与 Maven reactor。平台环境要求见 [项目总览](../readme.md)。

## 目录

| 文件 | 用途 |
| --- | --- |
| [docker-compose.yml](docker-compose.yml) | PostgreSQL、Redis、RabbitMQ，以及 app profile 下的 Admin、SAS、Web |
| [docker.ps1](docker.ps1) / [docker.sh](docker.sh) | Windows / Bash 构建、启动、停止入口 |
| [.env.example](.env.example) | Compose 环境变量模板 |
| [admin/Dockerfile](admin/Dockerfile) | Admin JAR 镜像 |
| [sas/Dockerfile](sas/Dockerfile) | SAS JAR 镜像 |
| [web/Dockerfile](web/Dockerfile) | Web 静态站点镜像 |
| [web/nginx.conf](web/nginx.conf) | SPA 路由和 SAS 反向代理 |

每个 Dockerfile 旁的 `Dockerfile.dockerignore` 限定构建上下文中的文件。三个镜像统一使用仓库根目录作为 context，只发送对应 JAR、dist 或 Nginx 配置。

## 使用

在仓库根目录执行：

```powershell
.\docker\docker.ps1 build
.\docker\docker.ps1 up infra
.\docker\docker.ps1 up
.\docker\docker.ps1 down

# 已有 JAR / dist 时跳过对应构建步骤
.\docker\docker.ps1 build -SkipMaven -SkipWeb
```

```bash
bash docker/docker.sh build
bash docker/docker.sh up infra
bash docker/docker.sh up
bash docker/docker.sh down
SKIP_MVN=1 SKIP_WEB=1 bash docker/docker.sh build
```

脚本根据自身路径定位仓库，可从其他工作目录通过完整路径调用。首次使用且根目录 `.env` 不存在时，从本目录模板复制；已有 `.env` 保持不变。Admin、SAS 挂载根目录 `config/` 到容器 `/app/config`，业务配置分别读 `admin.properties` / `sas.properties`（Compose 只保留端口与 `JAVA_OPTS`）。

`build` 先用 Maven 构建 Admin/SAS，再在仓库内 `omes-iomp-web-admin` 安装依赖并构建 `dist/`，最后构建镜像。前端构建使用空 `VITE_SAS_BASE_URL` 以支持同源代理；该步骤只运行 Vite，不包含类型检查。后端需要可用的 Era 依赖。跳过构建时，每个后端 target 目录必须有且仅有一个匹配的 JAR，前端需已有 `dist/index.html`。

## 直接使用 Compose / Docker

在仓库根目录执行，提前准备 `.env`：

```bash
docker compose --env-file .env -f docker/docker-compose.yml config --quiet
docker compose --env-file .env -f docker/docker-compose.yml --profile app build
docker compose --env-file .env -f docker/docker-compose.yml --profile app up -d
docker compose --env-file .env -f docker/docker-compose.yml --profile app down

# 单独构建镜像，最后的点代表仓库根目录
docker build -f docker/admin/Dockerfile -t omes-admin:local .
docker build -f docker/sas/Dockerfile -t omes-sas:local .
docker build -f docker/web/Dockerfile -t omes-web:local .
```

IDEA 的 `.run/` 配置已同步新路径，Dockerfile 构建 context 为仓库根目录。

## 网络与数据

默认端口为 Web 8080、SAS 9400、Admin 10010，具体映射以 Compose 和根目录 `.env` 为准。`down` 不带 `--volumes`，保留数据库等命名卷。

本次集中目录保留既有网络配置：Admin、SAS 通过 `host.docker.internal` 访问中间件与互相调用（见各服务 properties 中的 `db_*` / `REDIS_*` / `OMES_ADMIN_URL`），Web Nginx 通过 `host.docker.internal:9400` 访问 SAS。部署主机需支持该地址并开放对应端口；修改 SAS 宿主机映射端口时，同步调整 Nginx upstream。

当前 Compose 仅集成已有的 Admin、SAS、Web 和三种中间件。AI、Control、Flink 尚未定义容器服务，其运行说明见各模块文档。
