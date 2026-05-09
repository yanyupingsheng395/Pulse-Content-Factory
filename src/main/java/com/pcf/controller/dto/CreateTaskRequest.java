package com.pcf.controller.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class CreateTaskRequest {

    /**
     * 原始分享文本，需包含可解析的 URL。
     */
    @NotBlank
    private String text;

    private String platform = "douyin";

    /**
     * 可选：原视频标题，便于模型理解。
     */
    private String titleOriginal;

    /**
     * 可选：配音员 ID（MoneyPrinterTurbo voice_name），空则用设置中的默认。
     */
    private String voiceName;
}
