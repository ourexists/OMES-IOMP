# omes-iomp-flinker-workshop

公共构建环境见 [项目总览](../../readme.md)；设备流作业见 [omes-iomp-flinker-equip](../omes-iomp-flinker-equip/README.md)。聚合见 [omes-iomp-flinker](../README.md)。

场景实时流 Flink 作业：消费 RabbitMQ 场景实时队列（默认 `omes.workshop.realtime`，与门户 `workshopRealtime-out-0` 对齐），按可配置周期对 `needCollect` 属性做**采集快照**，写入桥接队列（默认 `omes.workshop.stream.persist.collect`）。门户侧 Spring Cloud Stream 消费该桥接队列、攒批后调用 `WorkshopCollectFeign#addBatch` 入库。

本目录是**处理模块**（作业图、算子、配置），本身不可启动。两种启动各是一个模块，集群为 JDK 17、Flink 1.20.1：

| 模块 | 方式 |
|------|------|
| [omes-iomp-flinker-workshop-boot](../omes-iomp-flinker-workshop-boot/README.md) | `java -jar`，Spring Boot 里嵌 Flink |
| [omes-iomp-flinker-workshop-flink](../omes-iomp-flinker-workshop-flink/README.md) | `flink run` / Web UI |

在父工程目录 `omes-iomp` 构建：

```bash
mvn -pl omes-iomp-flinker/omes-iomp-flinker-workshop-boot,omes-iomp-flinker/omes-iomp-flinker-workshop-flink -am clean package -DskipTests
```

## 提交

```bash
flink run -c com.ourexists.omes.stream.workshop.WorkshopRealtimeFlinkJob omes-iomp-flinker/omes-iomp-flinker-workshop-flink/target/omes-iomp-flinker-workshop-flink-1.0.1-SNAPSHOT.jar --RABBITMQ_HOST 127.0.0.1 --RABBITMQ_PORT 5672 --RABBITMQ_USERNAME admin --RABBITMQ_PASSWORD TyY6Df3bZe --RABBITMQ_VHOST / --omes.workshop.rabbitmq.workshop-realtime-queue omes.workshop.realtime --omes.workshop.rabbitmq.workshop-stream-persist-collect-queue omes.workshop.stream.persist.collect --omes.workshop.snapshot-interval-ms 30000 --omes.workshop.flink.enable-checkpointing false --omes.workshop.flink.checkpoint-interval-ms 10000 --omes.workshop.flink.checkpoint-timeout-ms 120000 --omes.workshop.flink.unaligned-checkpoint true --omes.workshop.flink.aligned-checkpoint-timeout-ms 30000 --omes.workshop.flink.rmq-prefetch 100 --omes.workshop.flink.local false
```

程序参数写在 jar 后面。Web UI 提交时，集群进程读不到你本机的环境变量，配置都放在 **Program Arguments**。

上传 `*-flink.jar` 后，提交表单按下表填：

| 字段 | 填写 |
|------|------|
| Entry Class | `com.ourexists.omes.stream.workshop.WorkshopRealtimeFlinkJob` |
| Parallelism | 留空则用集群默认并行度（Flink 默认 `1`）。这是集群作业并行度，不是本地模式的 `omes.workshop.flink.local.parallelism` |
| Program Arguments | 下面这一整段（按部署改 RabbitMQ；不要加 `--local true`） |
| Savepoint Path | 首次提交留空 |
| Allow Non Restored State | 不勾选。只有从与当前作业对不上的 savepoint 恢复时才勾选 |

Program Arguments（值与代码默认一致；`state-ttl` 默认 `-1` 表示关闭，不要写成程序参数，`ParameterTool` 会把以 `-` 开头的值当成下一个键）：

```
--RABBITMQ_HOST host.docker.internal --RABBITMQ_PORT 5672 --RABBITMQ_USERNAME admin --RABBITMQ_PASSWORD TyY6Df3bZe --RABBITMQ_VHOST / --omes.workshop.rabbitmq.workshop-realtime-queue omes.workshop.realtime --omes.workshop.rabbitmq.workshop-stream-persist-collect-queue omes.workshop.stream.persist.collect --omes.workshop.snapshot-interval-ms 30000 --omes.workshop.flink.enable-checkpointing false --omes.workshop.flink.checkpoint-interval-ms 10000 --omes.workshop.flink.checkpoint-timeout-ms 120000 --omes.workshop.flink.unaligned-checkpoint true --omes.workshop.flink.aligned-checkpoint-timeout-ms 30000 --omes.workshop.flink.rmq-prefetch 100 --omes.workshop.flink.local false
```

| 程序参数 | 含义 | 这段里的值 |
|----------|------|------------|
| `--RABBITMQ_HOST` | RabbitMQ 主机 | `host.docker.internal` |
| `--RABBITMQ_PORT` | 端口 | `5672` |
| `--RABBITMQ_USERNAME` | 用户名 | `admin` |
| `--RABBITMQ_PASSWORD` | 密码 | 与作业代码默认相同，生产改掉 |
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
| `--omes.workshop.flink.local` | 集群提交必须为 `false` | `false` |
| `--omes.workshop.flink.state-ttl-minutes.collect-snapshot` | 采集快照 state TTL（分钟），正数才传入 | 省略，默认 `-1`（关闭） |

## 本地调试（嵌入式 MiniCluster）

用于 IDE 断点或本机 `java -jar` 试跑：开启**本地模式**后使用嵌入式 Flink，而不是 `flink run` 连远程集群。

1. 用 [omes-iomp-flinker-workshop-boot](../omes-iomp-flinker-workshop-boot/README.md) 的 jar。不要把集群模块的 jar 拿来 `java -jar`。

2. 打开本地模式（任选其一）：

   - 程序参数：`--local true` 或 `--omes.workshop.flink.local true`
   - 环境变量：`OMES_FLINK_LOCAL=true`
   - JVM：`-Domes.workshop.flink.local=true`

3. 可选参数：

   | 变量 / 点分键 | 说明 | 默认 |
   |---------------|------|------|
   | `OMES_FLINK_LOCAL_PARALLELISM` / `omes.workshop.flink.local.parallelism` | 并行度 | `1` |
   | `OMES_FLINK_LOCAL_WEBUI` / `omes.workshop.flink.local.webui` | 是否起本地 Flink Dashboard；`false` 时用无 UI 的 `createLocalEnvironment` | `true` |

4. **IntelliJ IDEA（Java 17）**：在运行配置的 **VM options** 里加入 Flink 1.20.1 `config.yaml` 的 `env.java.opts.all`（缺省时 Kryo 序列化可能报 `InaccessibleObjectException`）：

   ```
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

   运行 [omes-iomp-flinker-workshop-boot](../omes-iomp-flinker-workshop-boot/README.md) 的 `WorkshopFlinkBootApplication`（强制本地模式）。IDE 跑 boot 模块。

   本机 `java -jar` 时，把同一组参数放在 `-jar` 前面，程序参数放在 jar 后面（仓库根目录）：

   ```bash
   java --add-exports=java.base/sun.net.util=ALL-UNNAMED --add-exports=java.rmi/sun.rmi.registry=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED --add-exports=java.security.jgss/sun.security.krb5=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.net=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.text=ALL-UNNAMED --add-opens=java.base/java.time=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.util.concurrent=ALL-UNNAMED --add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED --add-opens=java.base/java.util.concurrent.locks=ALL-UNNAMED -jar omes-iomp-flinker/omes-iomp-flinker-workshop-boot/target/omes-iomp-flinker-workshop-boot-1.0.1-SNAPSHOT.jar
   ```

5. 本地调试建议保持 **`OMES_FLINK_ENABLE_CHECKPOINTING=false`**（默认），与 RMQ Source checkpoint 说明一致；仍需可连通的 RabbitMQ 与队列配置。

## 配置说明

配置由 Flink `ParameterTool` 合并，同名键合并顺序（后者覆盖前者）：**程序参数** → **JVM `-D` 系统属性** → **环境变量**。下列同时给出环境变量名与点分键时，任一侧均可设置（点分键见代码中的读取顺序）。

### 场景队列与业务

| 变量 | 点分键（可选） | 说明 | 默认 |
|------|----------------|------|------|
| `OMES_WORKSHOP_REALTIME_QUEUE` | `omes.workshop.rabbitmq.workshop-realtime-queue` | 场景实时消费队列 | `omes.workshop.realtime` |
| `OMES_WORKSHOP_STREAM_PERSIST_COLLECT_QUEUE` | `omes.workshop.rabbitmq.workshop-stream-persist-collect-queue` | 采集快照桥接队列；空字符串则采集快照 Sink 不发送 | `omes.workshop.stream.persist.collect` |
| `OMES_WORKSHOP_SNAPSHOT_INTERVAL_MS` | `omes.workshop.snapshot-interval-ms` | 采集快照周期（毫秒） | `30000` |
| `OMES_FLINK_STATE_TTL_MINUTES_WORKSHOP_COLLECT_SNAPSHOT` | `omes.workshop.flink.state-ttl-minutes.collect-snapshot` | 采集快照算子 keyed state TTL（分钟）：`-1` 关闭，正数为空闲保留分钟 | `-1` |

### RabbitMQ 连接

| 变量 | 说明 | 默认 |
|------|------|------|
| `RABBITMQ_HOST` | 主机 | `127.0.0.1` |
| `RABBITMQ_PORT` | 端口 | `5672` |
| `RABBITMQ_USERNAME` | 用户名 | `admin` |
| `RABBITMQ_PASSWORD` | 密码（生产务必覆盖） | 部署时设置，默认值见作业配置代码 |
| `RABBITMQ_VHOST` | 虚拟主机 | `/` |

### Flink 与 RMQ Source

| 变量 | 点分键（可选） | 说明 | 默认 |
|------|----------------|------|------|
| `OMES_FLINK_ENABLE_CHECKPOINTING` | `omes.workshop.flink.enable-checkpointing` | 是否开启 checkpoint（`true` / `false`） | `false` |
| `OMES_FLINK_CHECKPOINT_MS` | `omes.workshop.flink.checkpoint-interval-ms` | checkpoint 间隔（毫秒） | `10000` |
| `OMES_FLINK_CHECKPOINT_TIMEOUT_MS` | `omes.workshop.flink.checkpoint-timeout-ms` | checkpoint 超时（毫秒） | `120000` |
| `OMES_FLINK_UNALIGNED_CHECKPOINT` | `omes.workshop.flink.unaligned-checkpoint` | 非对齐 checkpoint | `true` |
| `OMES_FLINK_ALIGNED_CHECKPOINT_TIMEOUT_MS` | `omes.workshop.flink.aligned-checkpoint-timeout-ms` | 对齐 barrier 超时后切非对齐（毫秒）；`0` 表示不设 | `30000` |
| `OMES_FLINK_RMQ_PREFETCH` | `omes.workshop.flink.rmq-prefetch` | RMQ prefetch | `100` |
| `OMES_FLINK_LOCAL` | `omes.workshop.flink.local` | `true`：嵌入式本地 Flink（调试）；另支持程序参数 `local` | `false` |
| `OMES_FLINK_LOCAL_PARALLELISM` | `omes.workshop.flink.local.parallelism` | 本地模式并行度 | `1` |
| `OMES_FLINK_LOCAL_WEBUI` | `omes.workshop.flink.local.webui` | 本地模式是否启用 Flink Web UI | `true` |

生产环境使用 RabbitMQ Source 时建议开启 checkpoint，否则异常恢复可能重复消费；详见作业启动日志中的提示。

## 内联模型

内联设备/场景模型与 `omes-iomp-device-base`、`omes-iomp-device-model` 同源字段变更时，请手动同步本模块 `com.ourexists.omes.device.*` 下源码。
