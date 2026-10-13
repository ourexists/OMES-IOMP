# omes-iomp-flinker-workshop-flink

场景流 **集群启动**。提交给 Flink，不含 Flink 本体。作业图见 [处理模块](../omes-iomp-flinker-workshop/README.md)。集群使用 **JDK 17**、Flink **1.20.1**。

## 构建

在父工程目录 `omes-iomp`：

```bash
mvn -pl omes-iomp-flinker/omes-iomp-flinker-workshop-flink -am clean package -DskipTests
```

产物：`omes-iomp-flinker/omes-iomp-flinker-workshop-flink/target/omes-iomp-flinker-workshop-flink-1.0.1-SNAPSHOT.jar`。不要拿本 jar 去 `java -jar`。

## 提交

Web UI / `flink run` 时，集群进程读不到你本机的环境变量，配置都放在 **Program Arguments**。不要加 `--local true`。

| 字段 | 填写 |
|------|------|
| Entry Class | `com.ourexists.omes.stream.workshop.WorkshopRealtimeFlinkJob` |
| Parallelism | 留空用集群默认。不是 `omes.workshop.flink.local.parallelism` |
| Program Arguments | 下面整段（按部署改 RabbitMQ；带 `--omes.workshop.flink.local false`） |
| Savepoint Path | 首次留空 |
| Allow Non Restored State | 不勾选 |

```bash
flink run -c com.ourexists.omes.stream.workshop.WorkshopRealtimeFlinkJob omes-iomp-flinker/omes-iomp-flinker-workshop-flink/target/omes-iomp-flinker-workshop-flink-1.0.1-SNAPSHOT.jar --RABBITMQ_HOST 127.0.0.1 --RABBITMQ_PORT 5672 --RABBITMQ_USERNAME admin --RABBITMQ_PASSWORD TyY6Df3bZe --RABBITMQ_VHOST / --omes.workshop.rabbitmq.workshop-realtime-queue omes.workshop.realtime --omes.workshop.rabbitmq.workshop-stream-persist-collect-queue omes.workshop.stream.persist.collect --omes.workshop.snapshot-interval-ms 30000 --omes.workshop.flink.enable-checkpointing false --omes.workshop.flink.checkpoint-interval-ms 10000 --omes.workshop.flink.checkpoint-timeout-ms 120000 --omes.workshop.flink.unaligned-checkpoint true --omes.workshop.flink.aligned-checkpoint-timeout-ms 30000 --omes.workshop.flink.rmq-prefetch 100 --omes.workshop.flink.local false
```

Program Arguments（Flink 在 Docker、宿主机 RabbitMQ 时主机可改成 `host.docker.internal`）：

```
--RABBITMQ_HOST host.docker.internal --RABBITMQ_PORT 5672 --RABBITMQ_USERNAME admin --RABBITMQ_PASSWORD TyY6Df3bZe --RABBITMQ_VHOST / --omes.workshop.rabbitmq.workshop-realtime-queue omes.workshop.realtime --omes.workshop.rabbitmq.workshop-stream-persist-collect-queue omes.workshop.stream.persist.collect --omes.workshop.snapshot-interval-ms 30000 --omes.workshop.flink.enable-checkpointing false --omes.workshop.flink.checkpoint-interval-ms 10000 --omes.workshop.flink.checkpoint-timeout-ms 120000 --omes.workshop.flink.unaligned-checkpoint true --omes.workshop.flink.aligned-checkpoint-timeout-ms 30000 --omes.workshop.flink.rmq-prefetch 100 --omes.workshop.flink.local false
```

## 配置合并顺序

无 `application.yml`。**程序参数** &lt; **JVM `-D`** &lt; **环境变量**。Web UI 通常只有程序参数。`state-ttl` 默认 `-1` 不要写成程序参数。

## 程序参数说明

| 程序参数 | 含义 | 这段里的值 |
|----------|------|------------|
| `--omes.workshop.flink.local` | 集群必须为 `false` | `false` |
| `--RABBITMQ_HOST` | RabbitMQ 主机 | `host.docker.internal` / `127.0.0.1` |
| `--RABBITMQ_PORT` | 端口 | `5672` |
| `--RABBITMQ_USERNAME` | 用户名 | `admin` |
| `--RABBITMQ_PASSWORD` | 密码 | 生产改掉 |
| `--RABBITMQ_VHOST` | 虚拟主机 | `/` |
| `--omes.workshop.rabbitmq.workshop-realtime-queue` | 场景实时消费队列 | `omes.workshop.realtime` |
| `--omes.workshop.rabbitmq.workshop-stream-persist-collect-queue` | 采集快照桥接队列；空字符串则不发送 | `omes.workshop.stream.persist.collect` |
| `--omes.workshop.snapshot-interval-ms` | 采集快照周期（毫秒） | `30000` |
| `--omes.workshop.flink.enable-checkpointing` | 是否开启 checkpoint | `false` |
| `--omes.workshop.flink.checkpoint-interval-ms` | checkpoint 间隔（毫秒） | `10000` |
| `--omes.workshop.flink.checkpoint-timeout-ms` | checkpoint 超时（毫秒） | `120000` |
| `--omes.workshop.flink.unaligned-checkpoint` | 非对齐 checkpoint | `true` |
| `--omes.workshop.flink.aligned-checkpoint-timeout-ms` | 对齐 barrier 超时后切非对齐（毫秒）；`0` 表示不设 | `30000` |
| `--omes.workshop.flink.rmq-prefetch` | RMQ prefetch | `100` |
| `--omes.workshop.flink.state-ttl-minutes.collect-snapshot` | 采集快照 state TTL（分钟），正数才传入 | 省略，默认 `-1` |

生产使用 RMQ Source 时建议开启 checkpoint。

## 环境变量对照（CLI `flink run` 时可用）

| 变量 | 点分键（可选） | 说明 | 默认 |
|------|----------------|------|------|
| `RABBITMQ_*` | 同名 | 连接 | 见上表 |
| `OMES_WORKSHOP_REALTIME_QUEUE` | `omes.workshop.rabbitmq.workshop-realtime-queue` | 实时队列 | `omes.workshop.realtime` |
| `OMES_WORKSHOP_STREAM_PERSIST_COLLECT_QUEUE` | `omes.workshop.rabbitmq.workshop-stream-persist-collect-queue` | 采集桥接 | `omes.workshop.stream.persist.collect` |
| `OMES_WORKSHOP_SNAPSHOT_INTERVAL_MS` | `omes.workshop.snapshot-interval-ms` | 快照周期 | `30000` |
| `OMES_FLINK_ENABLE_CHECKPOINTING` | `omes.workshop.flink.enable-checkpointing` | checkpoint 开关 | `false` |
| `OMES_FLINK_CHECKPOINT_MS` | `omes.workshop.flink.checkpoint-interval-ms` | checkpoint 间隔 | `10000` |
| `OMES_FLINK_CHECKPOINT_TIMEOUT_MS` | `omes.workshop.flink.checkpoint-timeout-ms` | checkpoint 超时 | `120000` |
| `OMES_FLINK_UNALIGNED_CHECKPOINT` | `omes.workshop.flink.unaligned-checkpoint` | 非对齐 | `true` |
| `OMES_FLINK_ALIGNED_CHECKPOINT_TIMEOUT_MS` | `omes.workshop.flink.aligned-checkpoint-timeout-ms` | 对齐超时 | `30000` |
| `OMES_FLINK_RMQ_PREFETCH` | `omes.workshop.flink.rmq-prefetch` | prefetch | `100` |
| `OMES_FLINK_STATE_TTL_MINUTES_WORKSHOP_COLLECT_SNAPSHOT` | `omes.workshop.flink.state-ttl-minutes.collect-snapshot` | TTL（`-1` 关闭） | `-1` |
