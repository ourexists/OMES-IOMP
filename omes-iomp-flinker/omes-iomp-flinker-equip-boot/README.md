# omes-iomp-flinker-equip-boot

设备流 **Spring Boot** 启动：进程内嵌入式 Flink，强制本地模式。作业图见 [处理模块](../omes-iomp-flinker-equip/README.md)。

## 构建与启动

字节码目标 **17**；本机可用 JDK 17 或 21（21 须加 `-Djava.security.manager=allow`）。在父工程目录 `omes-iomp`：

```bash
mvn -pl omes-iomp-flinker/omes-iomp-flinker-equip-boot -am clean package -DskipTests
```

主类 `com.ourexists.omes.stream.equip.EquipFlinkBootApplication`。JVM 参数（Flink 1.20.1 `env.java.opts.all` + `-Djava.security.manager=allow`）放在 `-jar` 前面，业务参数放在 jar 后面：

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

```bash
java -Djava.security.manager=allow --add-exports=java.base/sun.net.util=ALL-UNNAMED --add-exports=java.rmi/sun.rmi.registry=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED --add-exports=java.security.jgss/sun.security.krb5=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.net=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.text=ALL-UNNAMED --add-opens=java.base/java.time=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.util.concurrent=ALL-UNNAMED --add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED --add-opens=java.base/java.util.concurrent.locks=ALL-UNNAMED -jar omes-iomp-flinker/omes-iomp-flinker-equip-boot/target/omes-iomp-flinker-equip-boot-1.0.1-SNAPSHOT.jar
```

IDE：主类同上，VM options 用上面那组；Program arguments 用下面「程序参数」整段。本进程强制 `omes.device.flink.local=true`，写 `false` 也不会去连集群。不要拿本 jar 去 `flink run`。

## 配置合并顺序

**`application.yml`**（本模块 `src/main/resources`）&lt; **程序参数** &lt; **JVM `-D`** &lt; **环境变量**。不写的项用 yml 默认值。`state-ttl` 默认 `-1`（关闭）不要写成程序参数：`ParameterTool` 会把以 `-` 开头的值当成下一个键；要开启只传正整数分钟。

## 程序参数（可整段粘贴）

```
--RABBITMQ_HOST 127.0.0.1 --RABBITMQ_PORT 5672 --RABBITMQ_USERNAME admin --RABBITMQ_PASSWORD TyY6Df3bZe --RABBITMQ_VHOST / --omes.device.rabbitmq.equip-realtime-queue omes.equip.realtime --omes.device.rabbitmq.equip-notify-create-queue omes.notify.create --omes.device.rabbitmq.equip-stream-persist-change-queue omes.equip.stream.persist.change --omes.device.rabbitmq.equip-stream-persist-state-queue omes.equip.stream.persist.state --omes.device.rabbitmq.equip-stream-persist-collect-queue omes.equip.stream.persist.collect --omes.device.offline-timeout-ms 90000 --omes.device.snapshot-interval-ms 30000 --omes.device.attr-fluctuation-window-ms 90000 --omes.device.attr-fluctuation-slide-ms 5000 --omes.device.flink.parallelism 1 --omes.device.flink.enable-checkpointing false --omes.device.flink.checkpoint-interval-ms 10000 --omes.device.flink.checkpoint-timeout-ms 120000 --omes.device.flink.unaligned-checkpoint true --omes.device.flink.aligned-checkpoint-timeout-ms 30000 --omes.device.flink.rmq-prefetch 100 --omes.device.flink.change-detect-ingress-windowed false --omes.device.flink.change-detect-ingress-window-ms 60000 --omes.device.flink.change-detect-ingress-slide-ms 60000
```

| 程序参数 | 含义 | 默认 |
|----------|------|------|
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
| `--omes.device.flink.state-ttl-minutes.*` | 各算子 state TTL（分钟），正数才传入 | 省略，默认 `-1` |

本地建议保持 checkpoint 关闭。需可连通的 RabbitMQ。

## 环境变量对照

亦可用环境变量或点分键覆盖（后者覆盖前者见合并顺序）。

| 变量 | 说明 | 默认 |
|------|------|------|
| `RABBITMQ_HOST` / `PORT` / `USERNAME` / `PASSWORD` / `VHOST` | 连接 | 见上表 |
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
| `OMES_FLINK_PARALLELISM` | 嵌入式并行度 | `1` |
| `OMES_FLINK_RMQ_PREFETCH` | prefetch | `100` |
| `OMES_FLINK_STATE_TTL_MINUTES_*` | 各算子 TTL（`-1` 关闭） | `-1` |
| `OMES_FLINK_CHANGE_DETECT_INGRESS_WINDOWED` | 变化检测入口窗口 | `false` |
| `OMES_FLINK_CHANGE_DETECT_INGRESS_WINDOW_MS` | 窗口长度 | `60000` |
| `OMES_FLINK_CHANGE_DETECT_INGRESS_SLIDE_MS` | 滑动步长 | `60000` |
