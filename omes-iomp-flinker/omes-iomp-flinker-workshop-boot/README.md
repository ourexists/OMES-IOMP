# omes-iomp-flinker-workshop-boot

场景流的 **Spring Boot 启动**。进程内嵌入式 Flink，强制本地模式。作业与配置见 [处理模块](../omes-iomp-flinker-workshop/README.md)。

在父工程目录 `omes-iomp`：

```bash
mvn -pl omes-iomp-flinker/omes-iomp-flinker-workshop-boot -am clean package -DskipTests
java -jar omes-iomp-flinker/omes-iomp-flinker-workshop-boot/target/omes-iomp-flinker-workshop-boot-1.0.1-SNAPSHOT.jar
```

JDK 17。JVM 的 `--add-opens` / `--add-exports` 放在 `-jar` 前面，见处理模块说明。主类 `com.ourexists.omes.stream.workshop.WorkshopFlinkBootApplication`。
