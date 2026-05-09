package com.pcf;

import com.pcf.config.WorkDirApplicationContextInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class PcfApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(PcfApplication.class);
        app.addInitializers(new WorkDirApplicationContextInitializer());
        app.run(args);
    }
}
