package com.ourexists.omes.stream.workshop;

import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.java.utils.ParameterTool;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Starts the Flink pipeline in a dedicated thread so the Spring context stays up while {@code env.execute()} blocks.
 */
@Slf4j
@Component
class WorkshopFlinkJobRunner implements ApplicationRunner {

    private final ConfigurableEnvironment environment;
    private final ConfigurableApplicationContext context;

    WorkshopFlinkJobRunner(ConfigurableEnvironment environment, ConfigurableApplicationContext context) {
        this.environment = environment;
        this.context = context;
    }

    @Override
    public void run(ApplicationArguments args) {
        Thread t = new Thread(() -> runJob(args), "omes-flinker-workshop-job");
        t.setDaemon(false);
        t.setUncaughtExceptionHandler(
                (thread, ex) -> {
                    log.error("Uncaught exception in Flink job thread", ex);
                    shutdown(1);
                });
        t.start();
    }

    private void runJob(ApplicationArguments args) {
        /* Spring Boot fat-jar：异步 / RPC 反序列化若沿用 AppClassLoader 会找不到 BOOT-INF/lib 下的 Flink。 */
        Thread.currentThread().setContextClassLoader(WorkshopFlinkJobRunner.class.getClassLoader());
        try {
            Map<String, String> spring = WorkshopFlinkSpringDefaults.flatten(environment);
            ParameterTool pt = WorkshopRealtimeFlinkJob.parameterTool(args.getSourceArgs(), spring)
                    .mergeWith(ParameterTool.fromMap(Map.of("omes.workshop.flink.local", "true")));
            WorkshopRealtimeFlinkJob.run(pt);
            log.warn("Flink job returned without blocking; shutting down Spring context");
            shutdown(0);
        } catch (Exception e) {
            log.error("Flink job failed", e);
            shutdown(1);
        }
    }

    private void shutdown(int code) {
        int exitCode = SpringApplication.exit(context, () -> code);
        System.exit(exitCode);
    }
}
