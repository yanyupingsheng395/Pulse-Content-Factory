package com.pcf.service;

import com.pcf.config.PcfProperties;
import com.pcf.dao.PcfSettingsRepository;
import com.pcf.model.PcfSettingsEntity;
import com.pcf.util.StringUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private final PcfSettingsRepository repository;
    private final PcfProperties properties;

    @Transactional
    public PcfSettingsEntity getOrCreate() {
        return repository.findById(PcfSettingsEntity.SINGLETON_ID).orElseGet(this::createDefaults);
    }

    @Transactional
    public PcfSettingsEntity save(PcfSettingsEntity patch) {
        PcfSettingsEntity e = getOrCreate();
        if (patch.getDeepseekBaseUrl() != null) {
            e.setDeepseekBaseUrl(patch.getDeepseekBaseUrl().trim());
        }
        if (patch.getDeepseekApiKey() != null && !StringUtil.isBlank(patch.getDeepseekApiKey())) {
            e.setDeepseekApiKey(patch.getDeepseekApiKey().trim());
        }
        if (patch.getDeepseekModel() != null) {
            e.setDeepseekModel(patch.getDeepseekModel());
        }
        if (patch.getSiliconflowBaseUrl() != null) {
            e.setSiliconflowBaseUrl(patch.getSiliconflowBaseUrl());
        }
        if (patch.getSiliconflowApiKey() != null && !StringUtil.isBlank(patch.getSiliconflowApiKey())) {
            e.setSiliconflowApiKey(patch.getSiliconflowApiKey().trim());
        }
        if (patch.getSiliconflowModel() != null) {
            e.setSiliconflowModel(patch.getSiliconflowModel());
        }
        if (patch.getMptBaseUrl() != null) {
            e.setMptBaseUrl(patch.getMptBaseUrl());
        }
        if (patch.getLocalFootageDir() != null) {
            e.setLocalFootageDir(patch.getLocalFootageDir());
        }
        if (patch.getCookieDouyin() != null) {
            e.setCookieDouyin(patch.getCookieDouyin());
        }
        if (patch.getCookieXiaohongshu() != null) {
            e.setCookieXiaohongshu(patch.getCookieXiaohongshu());
        }
        if (patch.getDefaultVoiceName() != null) {
            e.setDefaultVoiceName(patch.getDefaultVoiceName());
        }
        if (patch.getDefaultVideoAspect() != null) {
            e.setDefaultVideoAspect(patch.getDefaultVideoAspect());
        }
        if (patch.getRenderMode() != null) {
            e.setRenderMode(patch.getRenderMode());
        }
        e.setUpdatedAt(Instant.now());
        return repository.save(e);
    }

    public String effectiveDeepseekBaseUrl() {
        PcfSettingsEntity e = getOrCreate();
        return firstNonBlank(e.getDeepseekBaseUrl(), properties.getDeepseek().getBaseUrl());
    }

    public String effectiveDeepseekApiKey() {
        PcfSettingsEntity e = getOrCreate();
        return firstNonBlank(e.getDeepseekApiKey(), properties.getDeepseek().getApiKey());
    }

    public String effectiveDeepseekModel() {
        PcfSettingsEntity e = getOrCreate();
        return firstNonBlank(e.getDeepseekModel(), properties.getDeepseek().getModel());
    }

    public String effectiveSiliconflowBaseUrl() {
        PcfSettingsEntity e = getOrCreate();
        return firstNonBlank(e.getSiliconflowBaseUrl(), properties.getSiliconflow().getBaseUrl());
    }

    public String effectiveSiliconflowApiKey() {
        PcfSettingsEntity e = getOrCreate();
        return firstNonBlank(e.getSiliconflowApiKey(), properties.getSiliconflow().getApiKey());
    }

    public String effectiveSiliconflowModel() {
        PcfSettingsEntity e = getOrCreate();
        return firstNonBlank(e.getSiliconflowModel(), nvl(properties.getSiliconflow().getModel()));
    }

    public String effectiveMptBaseUrl() {
        PcfSettingsEntity e = getOrCreate();
        return firstNonBlank(e.getMptBaseUrl(), properties.getMoneyprinterTurbo().getBaseUrl());
    }

    public String effectiveLocalFootageDir() {
        PcfSettingsEntity e = getOrCreate();
        return firstNonBlank(e.getLocalFootageDir(), properties.getLocalFootageDir());
    }

    public String effectiveDefaultVoice() {
        PcfSettingsEntity e = getOrCreate();
        return firstNonBlank(e.getDefaultVoiceName(), properties.getMoneyprinterTurbo().getDefaultVoiceName());
    }

    public String effectiveDefaultAspect() {
        PcfSettingsEntity e = getOrCreate();
        return firstNonBlank(e.getDefaultVideoAspect(), properties.getMoneyprinterTurbo().getDefaultVideoAspect());
    }

    public boolean isTurboMode() {
        PcfSettingsEntity e = getOrCreate();
        String mode = firstNonBlank(e.getRenderMode(), properties.getRenderMode());
        return "TURBO".equalsIgnoreCase(mode);
    }

    private PcfSettingsEntity createDefaults() {
        PcfSettingsEntity e = new PcfSettingsEntity();
        e.setId(PcfSettingsEntity.SINGLETON_ID);
        e.setUpdatedAt(Instant.now());
        return repository.save(e);
    }

    private static String firstNonBlank(String a, String b) {
        if (!StringUtil.isBlank(a)) {
            return a.trim();
        }
        return b == null ? "" : b;
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }
}
