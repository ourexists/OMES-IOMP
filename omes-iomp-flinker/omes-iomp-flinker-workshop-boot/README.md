# omes-iomp-flinker-workshop-boot

场景流 **Spring Boot** 启动：进程内嵌入式 Flink，强制本地模式。作业图见 [处理模块](../omes-iomp-flinker-workshop/README.md)。

## 构建与启动

JDK **17**。在父工程目录 `omes-iomp`：

```bash
mvn -pl omes-iomp-flinker/omes-iomp-flinker-workshop-boot -am clean package -DskipTests
```

主类 `com.ourexists.omes.stream.workshop.WorkshopFlinkBootApplication`。JVM 参数（Flink 1.20.1 `env.java.opts.all`）放在 `-jar` 前面，业务参数放在 jar 后面：

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

```bash
java --add-exports=java.base/sun.net.util=ALL-UNNAMED --add-exports=java.rmi/sun.rmi.registry=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED --add-exports=java.security.jgss/sun.security.krb5=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.net=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.text=ALL-UNNAMED --add-opens=java.base/java.time=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.util.concurrent=ALL-UNNAMED --add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED --add-opens=java.base/java.util.concurrent.locks=ALL-UNNAMED -jar omes-iomp-flinker/omes-iomp-flinker-workshop-boot/target/omes-iomp-flinker-workshop-boot-1.0.1-SNAPSHOT.jar
```

IDE：主类同上，VM options 用上面那组；Program arguments 用下面整段。本进程强制 `omes.workshop.flink.local=true`。不要拿本 jar 去 `flink run`。

## 配置合并顺序

**`application.yml`**（本模块 `src/main/resources`）&lt; **程序参数** &lt; **JVM `-D`** &lt; **环境变量**。不写的项用 yml 默认值。`state-ttl` 默认 `-1` 不要写成程序参数；要开启只传正整数分钟。

## 程序参数（可整段粘贴）

```
--RABBITMQ_HOST 127.0.0.1 --RABBITMQ_PORT 5672 --RABBITMQ_USERNAME admin --RABBITMQ_PASSWORD TyY6Df3bZe --RABBITMQ_VHOST / --omes.workshop.rabbitmq.workshop-realtime-queue omes.workshop.realtime --omes.workshop.rabbitmq.workshop-stream-persist-collect-queue omes.workshop.stream.persist.collect --omes.workshop.snapshot-interval-ms 30000 --omes.workshop.flink.enable-checkpointing false --omes.workshop.flink.checkpoint-interval-ms 10000 --omes.workshop.flink.checkpoint-timeout-ms 120000 --omes.workshop.flink.unaligned-checkpoint true --omes.workshop.flink.aligned-checkpoint-timeout-ms 30000 --omes.workshop.flink.rmq-prefetch 100 --omes.workshop.flink.local.parallelism 1 --omes.workshop.flink.local.webui true
```

| 程序参数 | 含义 | 默认 |
|----------|------|------|
| `--RABBITMQ_HOST` | RabbitMQ 主机 | `127.0.0.1` |
| `--RABBITMQ_PORT` | 端口 | `5672` |
| `--RABBITMQ_USERNAME` | 用户名 | `admin` |
| `--RABBITMQ_PASSWORD` | 密码 | 与 `application.yml` 相同，生产改掉 |
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
| `--omes.workshop.flink.local.parallelism` | 嵌入式并行度，必须为正 | `1` |
| `--omes.workshop.flink.local.webui` | 是否起本地 Flink Dashboard | `true` |
| `--omes.workshop.flink.state-ttl-minutes.collect-snapshot` | 采集快照 state TTL（分钟），正数才传入 | 省略，默认 `-1` |

本地建议保持 checkpoint 关闭。需可连通的 RabbitMQ。

## 环境变量对照

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
| `OMES_FLINK_LOCAL_PARALLELISM` | `omes.workshop.flink.local.parallelism` | 本地并行度 | `1` |
| `OMES_FLINK_LOCAL_WEBUI` | `omes.workshop.flink.local.webui` | 本地 Web UI | `true` |
| `OMES_FLINK_STATE_TTL_MINUTES_WORKSHOP_COLLECT_SNAPSHOT` | `omes.workshop.flink.state-ttl-minutes.collect-snapshot` | TTL（`-1` 关闭） | `-1` |
