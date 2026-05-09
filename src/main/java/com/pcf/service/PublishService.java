package com.pcf.service;

import com.pcf.config.PcfProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class PublishService {

    private final PcfProperties properties;
    private final DouyinPublishFlow douyinPublishFlow;

    public void publishIfEnabled(Path finalVideo, String title, String topics) {
        if (!properties.isPublishEnabled()) {
            return;
        }
        douyinPublishFlow.publish(finalVideo, title, topics);
    }

    /** 忽略 publishEnabled，由用户点击「手动发布」触发。 */
    public void publishManual(Path finalVideo, String title, String topics) {
        douyinPublishFlow.publish(finalVideo, title, topics);
    }
}
