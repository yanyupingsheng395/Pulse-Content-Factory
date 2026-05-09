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
}
