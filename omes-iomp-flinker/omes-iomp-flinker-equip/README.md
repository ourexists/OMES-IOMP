# omes-iomp-flinker-equip

公共构建环境见 [项目总览](../../readme.md)；场景流见 [omes-iomp-flinker-workshop](../omes-iomp-flinker-workshop/README.md)。聚合见 [omes-iomp-flinker](../README.md)。

设备实时流的**处理模块**（作业图、算子）。本身不可启动。配置与启动说明在各自启动模块：

| 模块 | 方式 |
|------|------|
| [omes-iomp-flinker-equip-boot](../omes-iomp-flinker-equip-boot/README.md) | `java -jar`，Spring Boot 嵌 Flink。JDK 17+ |
| [omes-iomp-flinker-equip-flink](../omes-iomp-flinker-equip-flink/README.md) | `flink run` / Web UI。集群 JDK 17、Flink 1.20.1 |

```bash
mvn -pl omes-iomp-flinker/omes-iomp-flinker-equip-boot,omes-iomp-flinker/omes-iomp-flinker-equip-flink -am clean package -DskipTests
```

内联 DTO 与门户对齐：门户侧模型变更时，手动同步 `src/main/java/com/ourexists/omes/device/`、`.../message/` 下相关源码。
