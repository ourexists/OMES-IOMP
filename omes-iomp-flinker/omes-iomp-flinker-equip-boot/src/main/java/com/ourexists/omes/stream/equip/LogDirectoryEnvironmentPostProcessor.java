package com.ourexists.omes.stream.equip;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 与 IDE 不同，{@code java -jar} 常在任意工作目录执行；在 Logback 写文件前创建 {@code logging.file.path}，避免启动失败。
 */
public class LogDirectoryEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String dir = environment.getProperty("logging.file.path");
        if (dir == null || dir.isBlank()) {
            return;
        }
        try {
            Path p = Paths.get(dir);
            if (!Files.isDirectory(p)) {
                Files.createDirectories(p);
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Cannot create logging.file.path directory \""
                            + dir
                            + "\"; set writable LOGGING_FILE_PATH or run from a cwd where this path can be created.",
                    e);
        }
    }
}
