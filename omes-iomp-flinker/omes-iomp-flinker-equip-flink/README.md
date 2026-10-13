# omes-iomp-flinker-equip-flink

设备流 **集群启动**。提交给 Flink，不含 Flink 本体。作业图见 [处理模块](../omes-iomp-flinker-equip/README.md)。集群 JobManager / TaskManager 使用 **JDK 17**、Flink **1.20.1**（与 workshop 相同）。

## 构建

在父工程目录 `omes-iomp`：

```bash
mvn -pl omes-iomp-flinker/omes-iomp-flinker-equip-flink -am clean package -DskipTests
```

产物：`omes-iomp-flinker/omes-iomp-flinker-equip-flink/target/omes-iomp-flinker-equip-flink-1.0.1-SNAPSHOT.jar`。不要拿本 jar 去 `java -jar`（里面没有 Flink 本体）。

## 提交

Web UI / `flink run` 时，集群进程读不到你本机的环境变量，配置都放在 **Program Arguments**。

| 字段 | 填写 |
|------|------|
| Entry Class | `com.ourexists.omes.stream.equip.EquipRealtimeFlinkJob` |
| Parallelism | 留空用集群默认。这是集群作业并行度，不是 `omes.device.flink.parallelism` |
| Program Arguments | 下面整段（按部署改 RabbitMQ；带 `--omes.device.flink.local false`） |
| Savepoint Path | 首次留空 |
| Allow Non Restored State | 不勾选 |

```bash
flink run -c com.ourexists.omes.stream.equip.EquipRealtimeFlinkJob omes-iomp-flinker/omes-iomp-flinker-equip-flink/target/omes-iomp-flinker-equip-flink-1.0.1-SNAPSHOT.jar --omes.device.flink.local false --RABBITMQ_HOST 127.0.0.1 --RABBITMQ_PORT 5672 --RABBITMQ_USERNAME admin --RABBITMQ_PASSWORD TyY6Df3bZe --RABBITMQ_VHOST / --omes.device.rabbitmq.equip-realtime-queue omes.equip.realtime --omes.device.rabbitmq.equip-notify-create-queue omes.notify.create --omes.device.rabbitmq.equip-stream-persist-change-queue omes.equip.stream.persist.change --omes.device.rabbitmq.equip-stream-persist-state-queue omes.equip.stream.persist.state --omes.device.rabbitmq.equip-stream-persist-collect-queue omes.equip.stream.persist.collect --omes.device.offline-timeout-ms 90000 --omes.device.snapshot-interval-ms 30000 --omes.device.attr-fluctuation-window-ms 90000 --omes.device.attr-fluctuation-slide-ms 5000 --omes.device.flink.enable-checkpointing false --omes.device.flink.checkpoint-interval-ms 10000 --omes.device.flink.checkpoint-timeout-ms 120000 --omes.device.flink.unaligned-checkpoint true --omes.device.flink.aligned-checkpoint-timeout-ms 30000 --omes.device.flink.rmq-prefetch 100 --omes.device.flink.change-detect-ingress-windowed false --omes.device.flink.change-detect-ingress-window-ms 60000 --omes.device.flink.change-detect-ingress-slide-ms 60000
```

## 配置合并顺序

无 `application.yml`。**程序参数** &lt; **JVM `-D`** &lt; **环境变量**（后者覆盖前者）。Web UI 通常只有程序参数。`state-ttl` 默认 `-1` 不要写成程序参数（`-` 会被当成下一个键）；要开启只传正整数分钟。

## 程序参数说明

| 程序参数 | 含义 | 这段里的值 |
|----------|------|------------|
| `--omes.device.flink.local` | 集群必须为 `false` | `false` |
| `--RABBITMQ_HOST` | RabbitMQ 主机 | `127.0.0.1` |
| `--RABBITMQ_PORT` | 端口 | `5672` |
| `--RABBITMQ_USERNAME` | 用户名 | `admin` |
| `--RABBITMQ_PASSWORD` | 密码 | 生产改掉 |
| `--RABBITMQ_VHOST` | 虚拟主机 | `/` |
| `--omes.device.rabbitmq.equip-realtime-queue` | 设备实时消费队列 | `omes.equip.realtime` |
| `--omes.device.rabbitmq.equip-notify-create-queue` | 告警通知队列；空字符串则不发送 | `omes.notify.create` |
| `--omes.device.rabbitmq.equip-stream-persist-change-queue` | 运行/报警/在线变更桥接队列 | `omes.equip.stream.persist.change` |
| `--omes.device.rabbitmq.equip-stream-persist-state-queue` | 状态周期快照桥接队列 | `omes.equip.stream.persist.state` |
| `--omes.device.rabbitmq.equip-stream-persist-collect-queue` | 采集周期快照桥接队列 | `omes.equip.stream.persist.collect` |
| `--omes.device.offline-timeout-ms` | 离线判定阈值（毫秒） | `90000` |
| `--omes.device.snapshot-interval-ms` | 快照周期（毫秒） | `30000` |
| `--omes.device.attr-fluctuation-window-ms` | 属性波动窗口（毫秒） | `90000` |
| `--omes.device.attr-fluctuation-slide-ms` | 属性波动滑动步长（毫秒） | `5000` |
| `--omes.device.flink.enable-checkpointing` | 是否开启 checkpoint | `false` |
| `--omes.device.flink.checkpoint-interval-ms` | checkpoint 间隔（毫秒） | `10000` |
| `--omes.device.flink.checkpoint-timeout-ms` | checkpoint 超时（毫秒） | `120000` |
| `--omes.device.flink.unaligned-checkpoint` | 非对齐 checkpoint | `true` |
| `--omes.device.flink.aligned-checkpoint-timeout-ms` | 对齐 barrier 超时后切非对齐（毫秒）；`0` 表示不设 | `30000` |
| `--omes.device.flink.rmq-prefetch` | RMQ prefetch | `100` |
| `--omes.device.flink.change-detect-ingress-windowed` | `true`：变化检测入口先经处理时间滑动窗口 + reduce | `false` |
| `--omes.device.flink.change-detect-ingress-window-ms` | 窗口长度（毫秒） | `60000` |
| `--omes.device.flink.change-detect-ingress-slide-ms` | 滑动步长（毫秒） | `60000` |
| `--omes.device.flink.state-ttl-minutes.*` | 各算子 state TTL（分钟），正数才传入 | 省略，默认 `-1` |

生产使用 RMQ Source 时建议开启 checkpoint。并行度用 Web UI 的 Parallelism，不要指望 `omes.device.flink.parallelism`（那只在嵌入式模式生效）。

## 环境变量对照（CLI `flink run` 时可用）

| 变量 | 说明 | 默认 |
|------|------|------|
| `RABBITMQ_*` | 连接 | 见上表 |
| `OMES_EQUIP_REALTIME_QUEUE` | 实时队列 | `omes.equip.realtime` |
| `OMES_EQUIP_NOTIFY_CREATE_QUEUE` | 告警队列 | `omes.notify.create` |
| `OMES_EQUIP_STREAM_PERSIST_CHANGE_QUEUE` | 变更桥接 | `omes.equip.stream.persist.change` |
| `OMES_EQUIP_STREAM_PERSIST_STATE_QUEUE` | 状态快照桥接 | `omes.equip.stream.persist.state` |
| `OMES_EQUIP_STREAM_PERSIST_COLLECT_QUEUE` | 采集快照桥接 | `omes.equip.stream.persist.collect` |
| `OMES_EQUIP_OFFLINE_TIMEOUT_MS` | 离线阈值 | `90000` |
| `OMES_EQUIP_SNAPSHOT_INTERVAL_MS` | 快照周期 | `30000` |
| `OMES_EQUIP_ATTR_FLUCTUATION_WINDOW_MS` | 波动窗口 | `90000` |
| `OMES_EQUIP_ATTR_FLUCTUATION_SLIDE_MS` | 波动步长 | `5000` |
| `OMES_FLINK_ENABLE_CHECKPOINTING` | checkpoint 开关 | `false` |
| `OMES_FLINK_CHECKPOINT_MS` | checkpoint 间隔 | `10000` |
| `OMES_FLINK_CHECKPOINT_TIMEOUT_MS` | checkpoint 超时 | `120000` |
| `OMES_FLINK_UNALIGNED_CHECKPOINT` | 非对齐 | `true` |
| `OMES_FLINK_ALIGNED_CHECKPOINT_TIMEOUT_MS` | 对齐超时 | `30000` |
| `OMES_FLINK_RMQ_PREFETCH` | prefetch | `100` |
| `OMES_FLINK_STATE_TTL_MINUTES_*` | 各算子 TTL（`-1` 关闭） | `-1` |
| `OMES_FLINK_CHANGE_DETECT_INGRESS_*` | 变化检测入口窗口 | 见上表 |
