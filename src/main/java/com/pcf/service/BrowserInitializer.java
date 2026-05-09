package com.pcf.service;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import com.pcf.config.PcfProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicReference;

@Component
@RequiredArgsConstructor
public class BrowserInitializer {

    private final PcfProperties properties;
    private final AtomicReference<Playwright> playwrightRef = new AtomicReference<>();
    private final AtomicReference<BrowserContext> contextRef = new AtomicReference<>();

    public synchronized BrowserContext persistentContext() {
        BrowserContext ctx = contextRef.get();
        if (ctx != null) {
            return ctx;
        }
        Playwright pw = Playwright.create();
        playwrightRef.set(pw);
        String dir = properties.getPlaywright().getUserDataDir();
        BrowserType.LaunchPersistentContextOptions opts = new BrowserType.LaunchPersistentContextOptions()
                .setHeadless(properties.getPlaywright().isHeadless());
        BrowserContext created = pw.chromium().launchPersistentContext(Paths.get(dir), opts);
        contextRef.set(created);
        return created;
    }

    @PreDestroy
    public void close() {
        BrowserContext ctx = contextRef.getAndSet(null);
        if (ctx != null) {
            ctx.close();
        }
        Playwright pw = playwrightRef.getAndSet(null);
        if (pw != null) {
            pw.close();
        }
    }
}
