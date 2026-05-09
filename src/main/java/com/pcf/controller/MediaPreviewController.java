package com.pcf.controller;

import com.pcf.config.PcfProperties;
import com.pcf.dao.ContentTaskRepository;
import com.pcf.model.ContentTask;
import com.pcf.util.StringUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequiredArgsConstructor
public class MediaPreviewController {

    private final ContentTaskRepository taskRepository;
    private final PcfProperties properties;

    @GetMapping("/preview/{id}/video")
    public ResponseEntity<?> previewVideo(@PathVariable Long id) {
        ContentTask task = taskRepository.findById(id).orElse(null);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        if (!StringUtil.isBlank(task.getLocalPath())) {
            Path root = Paths.get(properties.getWorkDir()).toAbsolutePath().normalize();
            Path file = Paths.get(task.getLocalPath()).toAbsolutePath().normalize();
            if (!file.startsWith(root) || !Files.isRegularFile(file)) {
                return ResponseEntity.status(403).build();
            }
            FileSystemResource res = new FileSystemResource(file);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFileName() + "\"")
                    .contentType(MediaType.parseMediaType("video/mp4"))
                    .body(res);
        }
        if (!StringUtil.isBlank(task.getRemoteVideoUrl())) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(task.getRemoteVideoUrl()))
                    .build();
        }
        return ResponseEntity.notFound().build();
    }
}
