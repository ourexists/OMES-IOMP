package com.ourexists.omes.stream.equip;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** {@code java -jar} 入口：进程内嵌入式 Flink。集群提交用 {@link EquipRealtimeFlinkJob#main}。 */
@SpringBootApplication
public class EquipFlinkBootApplication {

    public static void main(String[] args) {
        SpringApplication.run(EquipFlinkBootApplication.class, args);
    }
}
