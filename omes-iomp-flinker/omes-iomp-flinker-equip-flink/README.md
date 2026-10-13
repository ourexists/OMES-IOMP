# omes-iomp-flinker-equip-flink

设备流的 **集群启动**。提交给 Flink，不含 Flink 本体。作业与 Program Arguments 见 [处理模块](../omes-iomp-flinker-equip/README.md)。

在父工程目录 `omes-iomp`：

```bash
mvn -pl omes-iomp-flinker/omes-iomp-flinker-equip-flink -am clean package -DskipTests
```

产物 `omes-iomp-flinker/omes-iomp-flinker-equip-flink/target/omes-iomp-flinker-equip-flink-1.0.1-SNAPSHOT.jar`。

Web UI：Entry Class 填 `com.ourexists.omes.stream.equip.EquipRealtimeFlinkJob`。Parallelism 留空用集群默认。Savepoint Path 首次留空。不要勾选 Allow Non Restored State。集群 JobManager / TaskManager 使用 **JDK 21**。
