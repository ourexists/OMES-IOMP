# omes-iomp-flinker-equip-boot

设备流的 **Spring Boot 启动**。进程内嵌入式 Flink，强制本地模式。作业与配置见 [处理模块](../omes-iomp-flinker-equip/README.md)。

在父工程目录 `omes-iomp`：

```bash
mvn -pl omes-iomp-flinker/omes-iomp-flinker-equip-boot -am clean package -DskipTests
java -jar omes-iomp-flinker/omes-iomp-flinker-equip-boot/target/omes-iomp-flinker-equip-boot-1.0.1-SNAPSHOT.jar
```

JDK 21。JVM 参数（含 `-Djava.security.manager=allow`）放在 `-jar` 前面，业务参数放在 jar 后面，见处理模块说明。主类 `com.ourexists.omes.stream.equip.EquipFlinkBootApplication`。
