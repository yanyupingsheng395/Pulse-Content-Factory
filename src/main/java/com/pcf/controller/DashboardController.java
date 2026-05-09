package com.pcf.controller;

import com.pcf.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final SettingsService settingsService;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("turboMode", settingsService.isTurboMode());
        model.addAttribute("defaultVoice", settingsService.effectiveDefaultVoice());
        return "index";
    }
}
