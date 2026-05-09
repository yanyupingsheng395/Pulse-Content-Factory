package com.pcf.config;

import com.pcf.util.StringUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@EnableConfigurationProperties(PcfProperties.class)
@RequiredArgsConstructor
public class PcfConfiguration {

    private final PcfProperties properties;

    @PostConstruct
    public void ensureWorkDirs() throws IOException {
        Path root = Paths.get(properties.getWorkDir());
        Files.createDirectories(root);
        Files.createDirectories(root.resolve("temp/raw"));
        Files.createDirectories(root.resolve("output"));
        String ud = properties.getPlaywright().getUserDataDir();
        if (StringUtil.isBlank(ud)) {
            ud = root.resolve("playwright-user-data").toString();
            properties.getPlaywright().setUserDataDir(ud);
        }
        Files.createDirectories(Paths.get(ud));
    }
}
