package com.pcf.controller;

import com.pcf.model.PcfSettingsEntity;
import com.pcf.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    @GetMapping("/settings")
    public String settingsPage(Model model) {
        model.addAttribute("s", settingsService.getOrCreate());
        model.addAttribute("saved", false);
        return "settings";
    }

    @PostMapping("/settings")
    public String saveSettings(
            Model model,
            @RequestParam(required = false) String deepseekBaseUrl,
            @RequestParam(required = false) String deepseekApiKey,
            @RequestParam(required = false) String deepseekModel,
            @RequestParam(required = false) String siliconflowBaseUrl,
            @RequestParam(required = false) String siliconflowApiKey,
            @RequestParam(required = false) String siliconflowModel,
            @RequestParam(required = false) String mptBaseUrl,
            @RequestParam(required = false) String localFootageDir,
            @RequestParam(required = false) String cookieDouyin,
            @RequestParam(required = false) String cookieXiaohongshu,
            @RequestParam(required = false) String defaultVoiceName,
            @RequestParam(required = false) String defaultVideoAspect,
            @RequestParam(required = false) String renderMode
    ) {
        PcfSettingsEntity patch = new PcfSettingsEntity();
        patch.setDeepseekBaseUrl(deepseekBaseUrl);
        patch.setDeepseekApiKey(deepseekApiKey);
        patch.setDeepseekModel(deepseekModel);
        patch.setSiliconflowBaseUrl(siliconflowBaseUrl);
        patch.setSiliconflowApiKey(siliconflowApiKey);
        patch.setSiliconflowModel(siliconflowModel);
        patch.setMptBaseUrl(mptBaseUrl);
        patch.setLocalFootageDir(localFootageDir);
        patch.setCookieDouyin(cookieDouyin);
        patch.setCookieXiaohongshu(cookieXiaohongshu);
        patch.setDefaultVoiceName(defaultVoiceName);
        patch.setDefaultVideoAspect(defaultVideoAspect);
        patch.setRenderMode(renderMode);

        PcfSettingsEntity saved = settingsService.save(patch);
        model.addAttribute("s", saved);
        model.addAttribute("saved", true);
        return "settings";
    }
}
