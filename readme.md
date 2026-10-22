# OMES IOMP 工业设备管理平台

面向工业现场的设备运营和生产执行。场景、产品型号、网关和设备只建一次；实时监视、组态、巡检和生产订单都引用这套档案。仓库总览见 [上级 README](../README.md)，页面走查见 [管理端介绍](管理端介绍.md)。

自有代码、文档和配置采用 [Apache-2.0](LICENSE)。

![平台架构](架构图.png)

## 能力

### 场景、产品与设备

场景树用来给泵站、车间和产线分层。末级场景填写地址和经纬度，GIS 按这个点画出该现场的设备。每个场景还可以配置三样运行数据：场景级采集项（网关、映射字段、单位），远程 SCADA 画面，以及画布上的 2D 组态。

产品规定这类设备采集什么、如何报警、允许下发哪些控制量。型号才填写采集地址。已经被设备引用的型号不能删除。设备档案选择场景、产品和型号、网关，以及可选的健康评分模板。哪台设备走哪条连接，在设备上选择，网关页查看已绑定设备。

网关是一条协议连接，支持这些标识：

| 标识 | 行为 |
| --- | --- |
| `DGW` | 按 Cron 轮询 [omes-dgw](../omes-dgw/README.md)，写点走网关 HTTP |
| `MQTT` | 通用 MQTT 订阅 |
| `WINCC` | REST 轮询 WinCC |
| `Rest` | 通用 REST 轮询 |

连接启用后由管理服务拉起采集，已启用的连接在保存后热重载。

### 运行监视与控制

工作台给出设备总数和在线、离线、运行、停止、告警，以及近 7 日在线趋势和告警动态，可按场景筛选。设备大屏把同一套数据投到值班室，含地图点位、健康均分和分级事件。GIS 按末级场景的经纬度聚合该现场全部设备。没有经纬度的场景不出现在地图上，地图密钥在系统配置里维护。

实时页按场景铺设备卡片，可按在线、离线、运行、停止、报警筛选。单台设备分为档案、实时状态、历史曲线和运行日志。曲线可看原始值，或按 30 分钟、小时、天、月聚合均值、最小和最大。控制量在实时状态里填写并写入；写入由管理服务按该网关的协议直接下发。

运行、在线、报警分开留档。健康评分模板按统计窗口计算：报警次数和持续时长扣分，运行率和在线率加权加分，使用年限、累计运行和启停次数超过阈值后再扣分。总分落在健康、关注、预警、故障。设备档案关联了模板，才会进入这套计算。

### 巡检

巡检按产品把检查项变成可评分的任务。

1. 巡检项是公共题库，题型为选项、数值（可设上下限）、是/否。
2. 模板按产品载入检查项并设权重，按 100 分制评分。选项和是否按匹配给分，数值按区间给分。
3. 计划绑定一个模板，周期为每日、每周或每月，并指定场景。不选场景表示全部现场。启用后按周期生成任务，也可以手动生成。启用中的计划先停用再改。
4. 任务状态为待执行、执行中、已完成、已逾期。待执行的可以派工，已逾期的可以重新开始。
5. 巡检人员登记姓名、工号和手机，可关联已有账号，或单独开登录账号。
6. 记录按设备留下本次得分、检查项结果和规则分。

### 报表、任务与外部同步

场景报表读取场景采集项的历史，时间范围在页面上选择。数据同步查看分片事务的起止、状态、入参和异常。事务类型包括订单拉取、计划下发、产线推送、计划开工、订单变更和计划中止。失败的事务可以断点重入。

任务管理用 Cron 启停后台任务。内置类型覆盖设备健康评分、计划状态推进、计划入队、计划完工、订单远程同步和同步数据清理。

实时值由协议管理器写入缓存，同时发到 RabbitMQ。设备流、场景流的历史落库和由此产生的告警，依赖 [Flink 作业](omes-iomp-flinker/README.md) 消费后再回写。作业可用内嵌 `java -jar`，也可以提交到 Flink 1.20.1 集群。

### 物料与生产

原料按分类建档。配方写清原料组成和占比，保存前占比要凑满。BOM 在独立页面维护。设备能力是一套加工方案：绑定加工设备，再按原料写容量。同一台设备可以出现在不同方案里。

产线工艺描述一条线怎么加工：加工对象、原材料、版本、单次产量和工序步进间隔。工序在画布上排节点和连线，节点带工艺时长、设定温度，以及映射内存和偏移量。配好后可以把工艺下载到选定的 PLC。列表上的同步时间和下载时间用来确认是否已经下到现场。

生产订单把配方、设备能力和产线工艺收成批次。新建时填写订单与场景、配方及投料、批次数和预期时间、加工设备、产线工序。状态从待投产、部分投产、投产完成走到生产完成，也可以取消。待投产且尚未拆出子计划的草稿可以删除。已投产的单可以改期、改线、增量或减量。有批次正在执行时，取消要强制并填写操作人。

计划看板在投产之后使用：待排产排入队列，排队中调整先后，生产进度查看在制和完工。计划流转由 LiteFlow 驱动。工序模块另外提供 YAML 配方和仿真接口（开始、步进、信号、停止），仓库内示例为常温二段硫化；管理端没有单独的配方编辑页。

### 账号、消息与客服

系统按平台分账号，管理端和移动端使用不同平台。账户可以冻结、设有效期、分配角色。角色决定菜单和按钮。权限按平台维护成一棵树。部门与租户在服务端维护，请求通过租户路由头区分数据；管理端没有单独的租户列表页。

消息分普通和告警，可按周期向指定平台推送。浏览器经 SAS 的 WebSocket 中继接收。文件管理存放产品和设备档案使用的图片，文件落在本地目录，通过 `/files` 访问。

在线客服使用 WuKongIM。管理服务代发 HTTP（生产前端走 `/api/im/**`）。Web Nginx 另提供同源前缀 `/wkim/` 反代到 WuKongIM HTTP（`VITE_WKIM_BASE_URL=/wkim`）。管理端提供客服工作台，其他页面有悬浮会话。开关和地址在 `config/admin.properties` 的 `omes.im.enabled`、`omes.im.api-url`、`omes.im.secret`。编排文件在 [docker/im](docker/im/README.md)，宿主机只监听 `127.0.0.1:5001`。

### 移动端与 AI

移动端是 [uni-app x](omes-iomp-app/README.md)，面向小程序和 Android。默认底部为设备、场景、消息和我的，可查看设备详情、采集值、运行状态、场景组态和 GIS。巡检任务页已实现，默认 Tab 未挂出巡检入口。

[AI 服务](omes-iomp-ai/README.md) 独立部署，默认端口 `10011`。编排链 `equipment-orchestrator-flow` 先路由，再进入设备、数据、告警、调度四个代理，最后汇总。巡检知识库提供重建索引、入库和问答，默认向量库为 Qdrant，集合名 `omes_inspection_kb`。模型供应商支持 OpenAI、DeepSeek、通义、以及需自行配置兼容端点的 Anthropic 和 Gemini。管理端没有单独的知识库页面。

## 进程与端口

| 进程 | 主类 | 默认端口 | 何时需要 |
| --- | --- | --- | --- |
| `omes-iomp-runner-admin` | `com.ourexists.omes.AdminApp` | 10010 | 业务 API、协议采集、文件 |
| `omes-iomp-runner-sas` | `com.ourexists.omes.sas.SasApp` | 9400 | OAuth2 与 API 网关，浏览器只访问它 |
| 管理端开发服务器 | — | 5173 | 本地页面 |
| 移动端 H5 | — | 9990 | 本地 H5 |
| `omes-iomp-ai` | `com.ourexists.omes.App` | 10011 | 使用代理编排或知识库时 |
| Flink Web UI（Compose 映射） | — | 10018 | 集群模式查看作业 |

SAS 校验令牌后把业务 API 转发到 Admin，不托管前端静态页。令牌存在 Redis。Admin 信任网关带入的身份，两边的 `OMES_INTERNAL_SERVICE_KEY` 保持一致。登录使用验证码滑块，客户端为 `mes`。

`omes-iomp-runner-control` 仍在反应器里，消息消费未装配。控制写入走 Admin 的协议管理器，默认部署不必启动该进程。

## 模块

业务模块由 `omes-iomp-runner-admin` 聚合，不用逐个启动。域内 `model`、`feign`、`server` 分别是模型、调用接口和服务实现；设备和消息另有 `base`。

| 模块 | 职责 |
| --- | --- |
| `omes-iomp-device` | 场景、产品、网关、设备、采集、告警、健康、设备能力 |
| `omes-iomp-inspection` | 巡检项、模板、计划、任务、记录、人员 |
| `omes-iomp-mat` | 原料、分类、BOM |
| `omes-iomp-line` | 产线与工序流程 |
| `omes-iomp-process` | 工序配方与仿真 |
| `omes-iomp-mo` | 生产订单 |
| `omes-iomp-mps` | 主生产计划与队列 |
| `omes-iomp-ucenter` | 账户、角色、权限、平台、部门、租户、系统配置、客服 |
| `omes-iomp-message` | 站内消息与通知 |
| `omes-iomp-task` | 定时任务 |
| `omes-iomp-sync` | 外部系统同步事务 |
| `omes-iomp-flinker` | 设备流与场景流，各有处理模块、内嵌启动和集群提交 |
| `omes-iomp-web-admin` | Vue 管理端，Maven 不编译前端 |
| `omes-iomp-app` | uni-app x 移动端 |
| `omes-iomp-ai` | 代理编排与知识库 |

## 环境与构建

- 后端使用 JDK 21、Maven。父 POM 为 `com.ourexists.era:era-parent:2024.0.2.0-SNAPSHOT`，相对路径 `../era/pom.xml`。构建前准备 Era 源码或可解析的私有制品源。
- Flink 作业字节码目标为 17，集群使用 JDK 17、Flink 1.20.1。本机 `java -jar` 可用 JDK 17 或 21。
- 管理端为 Vue、TypeScript、Vite、Ant Design Vue。Node.js 需满足其锁文件中的 engines（`^22.18.0` 或 `>=24.11.0`）。移动端使用 HBuilderX。
- 默认数据库为 PostgreSQL。仓库另有 MySQL、SQL Server、SQLite 的 profile 文件。另需 Redis 和 RabbitMQ。AI 使用知识库时另需 Qdrant。对象存储不是默认文件方案，档案图片走本地目录。

认证服务已包含在本仓库中。以下命令在 `omes-iomp` 目录执行：

```bash
mvn -pl omes-iomp-runner-admin,omes-iomp-runner-sas -am clean package -DskipTests
mvn clean package -DskipTests
```

`-DskipTests` 只跳过测试。前端的安装和构建见 [管理端](omes-iomp-web-admin/README.md) 与 [移动端](omes-iomp-app/README.md)。

## 本地配置与启动

运行工作目录设为 `omes-iomp`。Admin、SAS 通过 `@PropertySource` 读取 `config/admin.properties`、`config/sas.properties`（可从同名 `*.example` 复制）。AI 读取 `config/config.properties`。认证配置在 `config/era-token.yml`。这些本地文件不纳入版本控制。

数据库变量：`db_host`、`db_schema`、`db_username`、`db_password`。

| 变量 | 用途 |
| --- | --- |
| `REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD` | Redis |
| `RABBITMQ_HOST`、`RABBITMQ_PORT`、`RABBITMQ_USERNAME`、`RABBITMQ_PASSWORD`、`RABBITMQ_VHOST` | RabbitMQ，采集与 Flink 作业保持一致 |
| `OMES_GATEWAY_PORT` | SAS 端口，默认 9400 |
| `OMES_ADMIN_URL` | SAS 转发的 Admin 地址 |
| `OMES_INTERNAL_SERVICE_KEY` | Admin 与 SAS 的内部调用密钥 |
| `AI_BRIDGE_INTERNAL_KEY` | AI 与 Admin 之间的短期票据密钥 |

```bash
java -jar omes-iomp-runner-admin/target/omes-iomp-runner-admin-1.0.1-SNAPSHOT.jar
java -jar omes-iomp-runner-sas/target/omes-iomp-runner-sas-1.0.1-SNAPSHOT.jar
```

然后按管理端说明启动前端。浏览器访问前端站点，认证和业务请求进入 SAS，再转发到 Admin。AI、Flink 按需单独启动，命令见各自 README。

## Docker

Dockerfile、Compose 和脚本在 [docker](docker/README.md)。在 `omes-iomp` 目录执行：

```powershell
.\docker\docker.ps1 build
.\docker\docker.ps1 up infra
.\docker\docker.ps1 up
.\docker\docker.ps1 down
```

Linux / macOS 使用 `bash docker/docker.sh` 和相同子命令。脚本读取 `omes-iomp` 根目录的 `.env`；该文件不存在时，从 `docker` 目录的环境变量模板复制。模板文件名以 [docker/README.md](docker/README.md) 为准。Compose 的 app profile 包含 PostgreSQL、Redis、RabbitMQ、Admin、SAS 和 Web，默认映射 Web `8080`、SAS `9400`、Admin `10010`。AI、Flink 和 WuKongIM 不在这组服务里。客服编排见 [docker/im](docker/im/README.md)。

## 相关文档

| 文档 | 内容 |
| --- | --- |
| [管理端介绍](管理端介绍.md) | 按页面说明值班、建档、巡检和生产怎么用 |
| [管理端开发](omes-iomp-web-admin/README.md) | Node、Vite、网关代理 |
| [移动端](omes-iomp-app/README.md) | HBuilderX 与接口地址 |
| [AI](omes-iomp-ai/README.md) | 编排、模型和向量库 |
| [Flink](omes-iomp-flinker/README.md) | 设备流、场景流的两种启动方式 |
| [Docker](docker/README.md) | 镜像、网络和 Compose |
| [贡献](CONTRIBUTING.md) | 提交前阅读 |

## 开源协议

本项目自有代码、文档和配置采用 [Apache License 2.0](LICENSE)（SPDX：`Apache-2.0`）。版权归属见 [NOTICE](NOTICE) 和各文件中的原始声明。第三方依赖、内置组件及另有声明的文件遵循各自许可证，见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。

允许在遵守协议的前提下使用、修改、分发和商业使用。分发时附带许可证副本，保留适用的版权、归属和 NOTICE，修改过的文件标明变更。软件按现状提供。Era 使用 AGPL-3.0，与本目录的 Apache-2.0 不同，分发包含 Era 的作品时同时遵守该许可证。

## 交流与支持

- 微信：m15026681077
- 邮件：434713950@163.com
- 项目介绍：https://blog.ourexists.site/2026/01/30/omes/
