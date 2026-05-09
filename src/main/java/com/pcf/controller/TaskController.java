package com.pcf.controller;

import com.pcf.controller.dto.CreateTaskRequest;
import com.pcf.dao.ContentTaskRepository;
import com.pcf.model.ContentTask;
import com.pcf.model.TaskStatus;
import com.pcf.service.VideoTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final VideoTaskService videoTaskService;
    private final ContentTaskRepository taskRepository;

    @GetMapping
    public List<ContentTask> list() {
        return taskRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateTaskRequest req) {
        try {
            ContentTask task = videoTaskService.createFromText(
                    req.getText(),
                    req.getPlatform(),
                    req.getTitleOriginal(),
                    req.getVoiceName()
            );
            videoTaskService.processTaskAsync(task.getId());
            Map<String, Object> body = new HashMap<>();
            body.put("id", task.getId());
            body.put("shareUrl", task.getShareUrl());
            body.put("status", task.getStatus());
            return ResponseEntity.status(HttpStatus.CREATED).body(body);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(singleError(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(singleError(e.getMessage()));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(singleError("链接已存在"));
        }
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<?> publish(@PathVariable Long id) {
        ContentTask task = taskRepository.findById(id).orElse(null);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        int st = task.getStatus() == null ? 0 : task.getStatus();
        if (st != TaskStatus.READY_TO_PUBLISH.getCode()) {
            return ResponseEntity.badRequest().body(singleError("仅「待分发」状态可手动发布，当前状态=" + st));
        }
        videoTaskService.publishManualAsync(id);
        return ResponseEntity.accepted().body(singleOk("已提交发布任务"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContentTask> get(@PathVariable Long id) {
        return taskRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private static Map<String, String> singleError(String message) {
        Map<String, String> m = new HashMap<>();
        m.put("error", message);
        return m;
    }

    private static Map<String, String> singleOk(String message) {
        Map<String, String> m = new HashMap<>();
        m.put("message", message);
        return m;
    }
}
