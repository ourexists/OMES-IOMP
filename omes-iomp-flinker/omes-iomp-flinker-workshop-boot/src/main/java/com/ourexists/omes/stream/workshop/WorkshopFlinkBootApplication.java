package com.ourexists.omes.stream.workshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** {@code java -jar} 入口：进程内嵌入式 Flink。集群提交用 {@link WorkshopRealtimeFlinkJob#main}。 */
@SpringBootApplication
public class WorkshopFlinkBootApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkshopFlinkBootApplication.class, args);
    }
}
