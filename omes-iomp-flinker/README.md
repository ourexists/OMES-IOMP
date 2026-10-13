# omes-iomp-flinker

Flink 流处理聚合模块。子模块：

| 模块 | 角色 |
|------|------|
| [omes-iomp-flinker-equip](omes-iomp-flinker-equip/README.md) | 设备流处理 |
| [omes-iomp-flinker-equip-boot](omes-iomp-flinker-equip-boot/README.md) | 设备流 `java -jar` |
| [omes-iomp-flinker-equip-flink](omes-iomp-flinker-equip-flink/README.md) | 设备流集群提交 |
| [omes-iomp-flinker-workshop](omes-iomp-flinker-workshop/README.md) | 场景流处理 |
| [omes-iomp-flinker-workshop-boot](omes-iomp-flinker-workshop-boot/README.md) | 场景流 `java -jar`（Spring Boot） |
| [omes-iomp-flinker-workshop-flink](omes-iomp-flinker-workshop-flink/README.md) | 场景流集群提交 |

在父工程目录 `omes-iomp`：

```bash
mvn -f omes-iomp-flinker/pom.xml clean package -DskipTests
```
