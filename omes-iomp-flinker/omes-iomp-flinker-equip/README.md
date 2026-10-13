# omes-iomp-flinker-equip

公共构建环境见 [项目总览](../../readme.md)；场景流作业见 [omes-iomp-flinker-workshop](../omes-iomp-flinker-workshop/README.md)。聚合见 [omes-iomp-flinker](../README.md)。

设备实时流的**处理模块**（作业图、算子、配置）。本身不可启动。两种启动各是一个模块：

| 模块 | 方式 |
|------|------|
| [omes-iomp-flinker-equip-boot](../omes-iomp-flinker-equip-boot/README.md) | `java -jar`，Spring Boot 里嵌 Flink。JDK 21 |
| [omes-iomp-flinker-equip-flink](../omes-iomp-flinker-equip-flink/README.md) | `flink run` / Web UI。集群也要 JDK 21 |

在父工程目录 `omes-iomp` 构建（`-am` 会带上本模块）：

```bash
mvn -pl omes-iomp-flinker/omes-iomp-flinker-equip-boot,omes-iomp-flinker/omes-iomp-flinker-equip-flink -am clean package -DskipTests
```

内联类来源说明（DTO 与门户对齐）：若门户侧模型变更，需手动同步 `src/main/java/com/ourexists/omes/device/`、`.../message/` 下相关源码。

## 运行

需可连通的 **RabbitMQ** 及与管理端一致的队列 / vhost。JVM 使用 Flink 1.20.1 `config.yaml` 的 `env.java.opts.all`。运行时是 **JDK 21**，还要加 `-Djava.security.manager=allow`（Flink 在 Java 17 以上会调用已废弃的 Security Manager；不加会在 MiniCluster 启动时失败）：

```
-Djava.security.manager=allow
--add-exports=java.base/sun.net.util=ALL-UNNAMED
--add-exports=java.rmi/sun.rmi.registry=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED
--add-exports=java.security.jgss/sun.security.krb5=ALL-UNNAMED
--add-opens=java.base/java.lang=ALL-UNNAMED
--add-opens=java.base/java.net=ALL-UNNAMED
--add-opens=java.base/java.io=ALL-UNNAMED
--add-opens=java.base/java.nio=ALL-UNNAMED
--add-opens=java.base/sun.nio.ch=ALL-UNNAMED
--add-opens=java.base/java.lang.reflect=ALL-UNNAMED
--add-opens=java.base/java.text=ALL-UNNAMED
--add-opens=java.base/java.time=ALL-UNNAMED
--add-opens=java.base/java.util=ALL-UNNAMED
--add-opens=java.base/java.util.concurrent=ALL-UNNAMED
--add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED
--add-opens=java.base/java.util.concurrent.locks=ALL-UNNAMED
```

仓库根目录：

```bash
java -Djava.security.manager=allow --add-exports=java.base/sun.net.util=ALL-UNNAMED --add-exports=java.rmi/sun.rmi.registry=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED --add-exports=java.security.jgss/sun.security.krb5=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.net=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.text=ALL-UNNAMED --add-opens=java.base/java.time=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.util.concurrent=ALL-UNNAMED --add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED --add-opens=java.base/java.util.concurrent.locks=ALL-UNNAMED -jar omes-iomp-flinker/omes-iomp-flinker-equip-boot/target/omes-iomp-flinker-equip-boot-1.0.1-SNAPSHOT.jar
```

这些参数必须写在 `-jar` 前面。PowerShell 可先设 `$env:JAVA_TOOL_OPTIONS` 为同一串（不含 `java`），再执行 `java -jar`。

### 集群提交

`*-flink.jar` 的 Entry Class 是 `com.ourexists.omes.stream.equip.EquipRealtimeFlinkJob`。Parallelism、Savepoint Path、Allow Non Restored State 与场景作业相同：并行度留空用集群默认，首次提交 Savepoint 留空，不勾选 Allow Non Restored State。Program Arguments 用下面「启动参数」那一段，并带上 `--omes.device.flink.local false`。Web UI 的 Parallelism 才是集群并行度；`omes.device.flink.parallelism` 只在嵌入式模式生效。

```bash
flink run -c com.ourexists.omes.stream.equip.EquipRealtimeFlinkJob omes-iomp-flinker/omes-iomp-flinker-equip-flink/target/omes-iomp-flinker-equip-flink-1.0.1-SNAPSHOT.jar --omes.device.flink.local false --RABBITMQ_HOST 127.0.0.1 --RABBITMQ_PORT 5672 --RABBITMQ_USERNAME admin --RABBITMQ_PASSWORD TyY6Df3bZe --RABBITMQ_VHOST / --omes.device.rabbitmq.equip-realtime-queue omes.equip.realtime --omes.device.rabbitmq.equip-notify-create-queue omes.notify.create --omes.device.rabbitmq.equip-stream-persist-change-queue omes.equip.stream.persist.change --omes.device.rabbitmq.equip-stream-persist-state-queue omes.equip.stream.persist.state --omes.device.rabbitmq.equip-stream-persist-collect-queue omes.equip.stream.persist.collect --omes.device.offline-timeout-ms 90000 --omes.device.snapshot-interval-ms 30000 --omes.device.attr-fluctuation-window-ms 90000 --omes.device.attr-fluctuation-slide-ms 5000 --omes.device.flink.enable-checkpointing false --omes.device.flink.checkpoint-interval-ms 10000 --omes.device.flink.checkpoint-timeout-ms 120000 --omes.device.flink.unaligned-checkpoint true --omes.device.flink.aligned-checkpoint-timeout-ms 30000 --omes.device.flink.rmq-prefetch 100 --omes.device.flink.change-detect-ingress-windowed false --omes.device.flink.change-detect-ingress-window-ms 60000 --omes.device.flink.change-detect-ingress-slide-ms 60000
```

### IDE

运行主类 **`com.ourexists.omes.stream.equip.EquipFlinkBootApplication`**。VM options 用上一节那组参数；Program arguments 用下面这一整段。`java -jar` 会强制本地模式，写 `--omes.device.flink.local false` 也不会改去连集群。

### 启动参数

本模块没有 Flink Web UI 提交页。JVM 参数放在 `-jar` 前面，业务配置放在 jar 后面。不写的项用 `application.yml` 里的默认值。`state-ttl` 默认 `-1`（关闭），不要写成程序参数：`ParameterTool` 会把以 `-` 开头的值当成下一个键；要开启时只传正整数分钟。

```
--RABBITMQ_HOST 127.0.0.1 --RABBITMQ_PORT 5672 --RABBITMQ_USERNAME admin --RABBITMQ_PASSWORD TyY6Df3bZe --RABBITMQ_VHOST / --omes.device.rabbitmq.equip-realtime-queue omes.equip.realtime --omes.device.rabbitmq.equip-notify-create-queue omes.notify.create --omes.device.rabbitmq.equip-stream-persist-change-queue omes.equip.stream.persist.change --omes.device.rabbitmq.equip-stream-persist-state-queue omes.equip.stream.persist.state --omes.device.rabbitmq.equip-stream-persist-collect-queue omes.equip.stream.persist.collect --omes.device.offline-timeout-ms 90000 --omes.device.snapshot-interval-ms 30000 --omes.device.attr-fluctuation-window-ms 90000 --omes.device.attr-fluctuation-slide-ms 5000 --omes.device.flink.parallelism 1 --omes.device.flink.enable-checkpointing false --omes.device.flink.checkpoint-interval-ms 10000 --omes.device.flink.checkpoint-timeout-ms 120000 --omes.device.flink.unaligned-checkpoint true --omes.device.flink.aligned-checkpoint-timeout-ms 30000 --omes.device.flink.rmq-prefetch 100 --omes.device.flink.change-detect-ingress-windowed false --omes.device.flink.change-detect-ingress-window-ms 60000 --omes.device.flink.change-detect-ingress-slide-ms 60000
```

| 程序参数 | 含义 | 这段里的值 |
|----------|------|------------|
| `--RABBITMQ_HOST` | RabbitMQ 主机 | `127.0.0.1` |
| `--RABBITMQ_PORT` | 端口 | `5672` |
| `--RABBITMQ_USERNAME` | 用户名 | `admin` |
| `--RABBITMQ_PASSWORD` | 密码 | 与 `application.yml` 相同，生产改掉 |
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
| `--omes.device.flink.parallelism` | 嵌入式 MiniCluster 并行度，必须为正 | `1` |
| `--omes.device.flink.enable-checkpointing` | 是否开启 checkpoint | `false` |
| `--omes.device.flink.checkpoint-interval-ms` | checkpoint 间隔（毫秒） | `10000` |
| `--omes.device.flink.checkpoint-timeout-ms` | checkpoint 超时（毫秒） | `120000` |
| `--omes.device.flink.unaligned-checkpoint` | 非对齐 checkpoint | `true` |
| `--omes.device.flink.aligned-checkpoint-timeout-ms` | 对齐 barrier 超时后切非对齐（毫秒）；`0` 表示不设 | `30000` |
| `--omes.device.flink.rmq-prefetch` | RMQ prefetch | `100` |
| `--omes.device.flink.change-detect-ingress-windowed` | `true`：变化检测入口先经处理时间滑动窗口 + reduce | `false` |
| `--omes.device.flink.change-detect-ingress-window-ms` | 窗口长度（毫秒），`windowed=true` 时必须为正 | `60000` |
| `--omes.device.flink.change-detect-ingress-slide-ms` | 滑动步长（毫秒） | `60000` |
| `--omes.device.flink.state-ttl-minutes.offline-detect` | 离线检测 state TTL（分钟），正数才传入 | 省略，默认 `-1` |
| `--omes.device.flink.state-ttl-minutes.change-detect` | 变更检测 state TTL | 省略，默认 `-1` |
| `--omes.device.flink.state-ttl-minutes.attr-fluctuation` | 属性波动 state TTL | 省略，默认 `-1` |
| `--omes.device.flink.state-ttl-minutes.state-snapshot` | 状态周期快照 state TTL | 省略，默认 `-1` |
| `--omes.device.flink.state-ttl-minutes.collect-snapshot` | 采集周期快照 state TTL | 省略，默认 `-1` |

## 配置

合并优先级（后者覆盖前者）：**`application.yml` 中的键** &lt; **程序参数** &lt; **JVM `-D`** &lt; **操作系统环境变量**。业务默认值集中在 **`src/main/resources/application.yml`**（`omes.device.*`、`RABBITMQ_*`）；下表为说明及与 `OMES_*` 等环境变量的对应关系，便于在部署时覆盖。

### RabbitMQ

| 变量                  | 说明                         | 默认           |
|---------------------|----------------------------|--------------|
| `RABBITMQ_HOST`     | 主机                         | `127.0.0.1`  |
| `RABBITMQ_PORT`     | 端口                         | `5672`       |
| `RABBITMQ_USERNAME` | 用户名                        | `admin`      |
| `RABBITMQ_PASSWORD` | 密码（生产务必覆盖）                 | 部署时设置，默认值见 application.yml |
| `RABBITMQ_VHOST`    | 虚拟主机                       | `/`          |

### 队列与 Flink

| 变量                                              | 说明                                                                 | 默认 |
|-------------------------------------------------|--------------------------------------------------------------------|------|
| `OMES_EQUIP_REALTIME_QUEUE`                     | 设备实时消费队列                                                           | `omes.equip.realtime` |
| `OMES_EQUIP_NOTIFY_CREATE_QUEUE`                | 告警通知队列；设为空字符串则告警 Sink 不发送                                        | `omes.notify.create` |
| `OMES_EQUIP_STREAM_PERSIST_CHANGE_QUEUE`        | 运行/报警/在线变更桥接队列                                                    | `omes.equip.stream.persist.change` |
| `OMES_EQUIP_STREAM_PERSIST_STATE_QUEUE`         | 状态周期快照桥接队列                                                         | `omes.equip.stream.persist.state` |
| `OMES_EQUIP_STREAM_PERSIST_COLLECT_QUEUE`       | 采集周期快照桥接队列                                                         | `omes.equip.stream.persist.collect` |
| `OMES_FLINK_ENABLE_CHECKPOINTING`               | 是否开启 checkpoint（`true`/`false`）                                    | `false` |
| `OMES_FLINK_CHECKPOINT_MS`                      | checkpoint 间隔（毫秒）                                                  | `10000` |
| `OMES_FLINK_CHECKPOINT_TIMEOUT_MS`              | checkpoint 超时（毫秒）                                                  | `120000` |
| `OMES_FLINK_UNALIGNED_CHECKPOINT`               | 非对齐 checkpoint                                                       | `true` |
| `OMES_FLINK_ALIGNED_CHECKPOINT_TIMEOUT_MS`      | 对齐 barrier 超时后切非对齐（毫秒）；`0` 表示不设                              | `30000` |
| `OMES_FLINK_PARALLELISM`                        | 嵌入式 MiniCluster 默认并行度                                                | `1` |
| `OMES_FLINK_RMQ_PREFETCH`                       | RMQ prefetch                                                       | `100` |
| `OMES_FLINK_STATE_TTL_MINUTES_OFFLINE_DETECT`   | 离线检测算子 keyed state TTL：`-1` 不启用，正整数为空闲保留 **分钟**                  | `-1` |
| `OMES_FLINK_STATE_TTL_MINUTES_CHANGE_DETECT`    | 变更检测算子（同上）                                                        | `-1` |
| `OMES_FLINK_STATE_TTL_MINUTES_ATTR_FLUCTUATION` | 属性波动 MapState（同上）                                                  | `-1` |
| `OMES_FLINK_STATE_TTL_MINUTES_STATE_SNAPSHOT`   | 状态周期快照算子（同上）                                                      | `-1` |
| `OMES_FLINK_STATE_TTL_MINUTES_COLLECT_SNAPSHOT` | 采集周期快照算子（同上）                                                      | `-1` |

亦支持点分键，例如：`omes.device.flink.enable-checkpointing`、`omes.device.flink.parallelism`。

### 变化检测入口（可选窗口）

| 变量 | 说明 | 默认 |
|------|------|------|
| `OMES_FLINK_CHANGE_DETECT_INGRESS_WINDOWED` | `true`：变化检测入口先经处理时间滑动窗口 + reduce | `false` |
| `OMES_FLINK_CHANGE_DETECT_INGRESS_WINDOW_MS` | 窗口长度（毫秒），`windowed=true` 时必须为正 | `60000` |
| `OMES_FLINK_CHANGE_DETECT_INGRESS_SLIDE_MS` | 滑动步长（毫秒） | `60000` |

### 业务参数

| 变量                                      | 说明                    | 默认 |
|-----------------------------------------|-----------------------|------|
| `OMES_EQUIP_OFFLINE_TIMEOUT_MS`         | 离线判定阈值（毫秒）          | `90000` |
| `OMES_EQUIP_SNAPSHOT_INTERVAL_MS`       | 快照周期（毫秒）              | `30000` |
| `OMES_EQUIP_ATTR_FLUCTUATION_WINDOW_MS` | 属性波动窗口（毫秒）            | `90000` |
| `OMES_EQUIP_ATTR_FLUCTUATION_SLIDE_MS`  | 属性波动滑动步长（毫秒）          | `5000` |

## 常见问题

1. **DTO 与设备缓存模型不一致** — 内联类与 `omes-iomp-device`、`omes-iomp-message` 字段变更时请对齐。
2. **Keyed state TTL** — 各算子分别配置；默认 **`-1`** 表示不启用 State TTL。
3. **Checkpoint** — 默认关闭；开启前需理解 RMQ Source 与 checkpoint 对 ack 的影响（见日志说明）。
