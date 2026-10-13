# omes-iomp-flinker-workshop

公共构建环境见 [项目总览](../../readme.md)；设备流见 [omes-iomp-flinker-equip](../omes-iomp-flinker-equip/README.md)。聚合见 [omes-iomp-flinker](../README.md)。

场景实时流的**处理模块**（作业图、算子）：消费 `omes.workshop.realtime`，周期输出采集快照到 `omes.workshop.stream.persist.collect`。本身不可启动。配置与启动说明在各自启动模块（JDK 17、Flink 1.20.1）：

| 模块 | 方式 |
|------|------|
| [omes-iomp-flinker-workshop-boot](../omes-iomp-flinker-workshop-boot/README.md) | `java -jar`，Spring Boot 嵌 Flink |
| [omes-iomp-flinker-workshop-flink](../omes-iomp-flinker-workshop-flink/README.md) | `flink run` / Web UI |

```bash
mvn -pl omes-iomp-flinker/omes-iomp-flinker-workshop-boot,omes-iomp-flinker/omes-iomp-flinker-workshop-flink -am clean package -DskipTests
```

内联模型与 `omes-iomp-device-base` / `omes-iomp-device-model` 同源字段变更时，手动同步本模块 `com.ourexists.omes.device.*`。
