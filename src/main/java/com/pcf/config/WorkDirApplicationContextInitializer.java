package com.pcf.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 在 DataSource / JPA 初始化之前创建工作目录，否则 SQLite 不会因父目录不存在而自动创建库文件。
 */
public class WorkDirApplicationContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        Environment env = applicationContext.getEnvironment();
        String workDir = env.getProperty("pcf.work-dir");
        if (workDir == null || workDir.trim().isEmpty()) {
            workDir = env.getProperty("user.home", System.getProperty("user.home", ".")) + "/.pcf";
        }
        try {
            Files.createDirectories(Paths.get(workDir));
        } catch (IOException e) {
            throw new IllegalStateException("无法创建工作目录: " + workDir, e);
        }
    }
}
