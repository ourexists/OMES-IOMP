# omes-iomp-flinker-workshop

公共构建环境见 [项目总览](../readme.md)；设备流作业见 [omes-iomp-flinker-equip](../omes-iomp-flinker-equip/README.md)。

场景实时流 Flink 作业：消费 RabbitMQ 场景实时队列（默认 `omes.workshop.realtime`，与门户 `workshopRealtime-out-0` 对齐），按可配置周期对 `needCollect` 属性做**采集快照**，写入桥接队列（默认 `omes.workshop.stream.persist.collect`）。门户侧 Spring Cloud Stream 消费该桥接队列、攒批后调用 `WorkshopCollectFeign#addBatch` 入库。

## 构建

在仓库根目录执行：

```bash
mvn -pl omes-iomp-flinker-workshop clean package -DskipTests
```

产物：`omes-iomp-flinker-workshop/target/omes-iomp-flinker-workshop-*-flink.jar`（`Main-Class`：`com.ourexists.omes.stream.workshop.WorkshopRealtimeFlinkJob`）。

默认 `flink-streaming-java`、`flink-clients` 为 **provided**，由 Flink 集群提供，须与 `pom.xml` 中 **`flink.version`（当前 1.20.1）** 一致；本模块 **`maven.compiler.release` 为 17**，集群 JobManager / TaskManager 请使用 **JDK 17**。

本地或 IDE 试跑需要把 Flink 打进 classpath 时：

```bash
mvn -pl omes-iomp-flinker-workshop clean package -DskipTests -Pflink-with-dependencies
```

## 提交

```bash
flink run -c com.ourexists.omes.stream.workshop.WorkshopRealtimeFlinkJob /path/to/omes-iomp-flinker-workshop-*-flink.jar
```

Flink Web UI：上传 `*-flink.jar`，Main Class 填上述全限定名。

## 本地调试（嵌入式 MiniCluster）

用于 IDE 断点或本机 `java -jar` 试跑：开启**本地模式**后使用嵌入式 Flink，而不是 `flink run` 连远程集群。

1. 打包时带上 Flink 依赖（**不要**用该 profile 打提交到生产集群的 fat JAR，以免与集群 Flink 重复打包核心类）：

   ```bash
   mvn -pl omes-iomp-flinker-workshop clean package -DskipTests -Pflink-with-dependencies
   ```

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

   **Program arguments**：`--local true`。Maven 运行配置勾选 profile **`flink-with-dependencies`**，使 Flink 依赖进入 classpath。

   本机 `java -jar` 时，把同一组参数放在 `-jar` 前面，程序参数放在 jar 后面（仓库根目录）：

   ```bash
   java --add-exports=java.base/sun.net.util=ALL-UNNAMED --add-exports=java.rmi/sun.rmi.registry=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED --add-exports=java.security.jgss/sun.security.krb5=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.net=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.text=ALL-UNNAMED --add-opens=java.base/java.time=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.util.concurrent=ALL-UNNAMED --add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED --add-opens=java.base/java.util.concurrent.locks=ALL-UNNAMED -jar omes-iomp-flinker-workshop/target/omes-iomp-flinker-workshop-1.0.1-SNAPSHOT-flink.jar --local true
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
