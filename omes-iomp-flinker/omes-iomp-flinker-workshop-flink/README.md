# omes-iomp-flinker-workshop-flink

场景流的 **集群启动**。提交给 Flink，不含 Flink 本体。作业与 Program Arguments 见 [处理模块](../omes-iomp-flinker-workshop/README.md)。

在父工程目录 `omes-iomp`：

```bash
mvn -pl omes-iomp-flinker/omes-iomp-flinker-workshop-flink -am clean package -DskipTests
```

产物 `omes-iomp-flinker/omes-iomp-flinker-workshop-flink/target/omes-iomp-flinker-workshop-flink-1.0.1-SNAPSHOT.jar`。

Web UI：Entry Class 填 `com.ourexists.omes.stream.workshop.WorkshopRealtimeFlinkJob`。Parallelism 留空用集群默认。Savepoint Path 首次留空。不要勾选 Allow Non Restored State。不要加 `--local true`。集群使用 JDK 17、Flink 1.20.1。
