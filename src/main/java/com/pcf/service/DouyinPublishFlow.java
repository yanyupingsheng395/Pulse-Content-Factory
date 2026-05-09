package com.pcf.service;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.pcf.config.PcfProperties;
import com.pcf.util.StringUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
public class DouyinPublishFlow {

    private static final Logger log = LoggerFactory.getLogger(DouyinPublishFlow.class);

    private final PcfProperties properties;
    private final BrowserInitializer browserInitializer;

    public void publish(Path videoFile, String title, String topics) {
        PcfProperties.Publish.Douyin d = properties.getPublish().getDouyin();
        if (StringUtil.isBlank(d.getUploadButtonSelector())) {
            log.warn("未配置 douyin.upload-button-selector，跳过自动上传。请打开创作者中心手动完成。");
            openCreatorPageOnly();
            return;
        }
        BrowserContext ctx = browserInitializer.persistentContext();
        Page page = ctx.pages().isEmpty() ? ctx.newPage() : ctx.pages().get(0);
        page.navigate(d.getCreatorUrl());
        humanDelay();
        Locator upload = page.locator(d.getUploadButtonSelector());
        upload.click();
        humanDelay();
        page.setInputFiles("input[type='file']", videoFile);
        humanDelay();
        if (!StringUtil.isBlank(d.getTitleInputSelector())) {
            page.locator(d.getTitleInputSelector()).fill(title == null ? "" : title);
            humanDelay();
        }
        if (!StringUtil.isBlank(topics) && !StringUtil.isBlank(d.getTopicInputSelector())) {
            page.locator(d.getTopicInputSelector()).fill(topics);
            humanDelay();
        }
        log.info("抖音发布流程已执行到填表阶段，后续发布按钮请视平台 UI 二次扩展选择器。");
    }

    private void openCreatorPageOnly() {
        BrowserContext ctx = browserInitializer.persistentContext();
        Page page = ctx.pages().isEmpty() ? ctx.newPage() : ctx.pages().get(0);
        page.navigate(properties.getPublish().getDouyin().getCreatorUrl());
    }

    private void humanDelay() {
        long min = properties.getPublish().getMinDelayMs();
        long max = properties.getPublish().getMaxDelayMs();
        long hi = Math.max(min, max);
        long lo = Math.min(min, max);
        long sleep = lo + (long) (ThreadLocalRandom.current().nextDouble() * (hi - lo + 1));
        try {
            Thread.sleep(sleep);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
