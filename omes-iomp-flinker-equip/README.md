# omes-iomp-flinker-equip

公共构建环境见 [项目总览](../readme.md)；场景流作业见 [omes-iomp-flinker-workshop](../omes-iomp-flinker-workshop/README.md)。

设备实时流处理：在 **Spring Boot** 进程内启动 **嵌入式 Flink 本地执行环境**（`createLocalEnvironment`），消费 RabbitMQ 设备实时队列，完成离线检测、变化检测、快照与属性波动告警，并回写 RabbitMQ。**不提供** `flink run` / 向独立 Flink 集群提交作业。

### 单 JAR 部署（无需单独安装 Flink）

- **只需要** 本模块 `mvn package` 打出的 **一个** Spring Boot Fat JAR；用 `java -jar`启动即可。
- **不需要** 在操作系统上安装 Apache Flink 发行版、**不需要** 配置 `FLINK_HOME`、**不需要** 在服务器上再部署一份 Flink 的 `lib`；Flink 及 RabbitMQ 连接器已作为普通 Maven 依赖打入 Fat JAR 的 `BOOT-INF/lib`，由 Spring Boot 启动器加载，与业务代码同进程运行。
- **仍需要**：**JDK 21**、可连通的 **RabbitMQ**（及与管理端一致的队列 / vhost），以及下文的 JVM 参数（Flink 1.20.1 的 `env.java.opts.all`，外加 `-Djava.security.manager=allow`）。

本模块 `maven.compiler.release` 为 **21**（运行时请使用 JDK 21；Flink 1.20 支持 Java 21）。构建产物为 **单个可执行 Spring Boot JAR**。

## 构建

仓库根目录：

```bash
mvn -pl omes-iomp-flinker-equip clean package -DskipTests
```

产物：`omes-iomp-flinker-equip/target/omes-iomp-flinker-equip-<version>.jar`（Fat JAR，`Main-Class` 为 Spring Boot loader）。

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
java -Djava.security.manager=allow --add-exports=java.base/sun.net.util=ALL-UNNAMED --add-exports=java.rmi/sun.rmi.registry=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED --add-exports=java.security.jgss/sun.security.krb5=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.net=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.text=ALL-UNNAMED --add-opens=java.base/java.time=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.util.concurrent=ALL-UNNAMED --add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED --add-opens=java.base/java.util.concurrent.locks=ALL-UNNAMED -jar omes-iomp-flinker-equip/target/omes-iomp-flinker-equip-1.0.1-SNAPSHOT.jar
```

这些参数必须写在 `-jar` 前面。PowerShell 可先设 `$env:JAVA_TOOL_OPTIONS` 为同一串（不含 `java`），再执行 `java -jar`。

### IDE

运行主类 **`com.ourexists.omes.stream.equip.EquipFlinkBootApplication`**，VM options 同上。

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
