package com.pcf.service;

import com.pcf.config.PcfProperties;
import com.pcf.util.ProcessRunner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class DownloadService {

    private final PcfProperties properties;

    public Path downloadVideo(long taskId, String pageUrl) throws IOException, InterruptedException {
        Path dir = Paths.get(properties.getWorkDir(), "temp", "raw", String.valueOf(taskId));
        Files.createDirectories(dir);
        Path template = dir.resolve("source.%(ext)s");
        List<String> cmd = Arrays.asList(
                properties.getYtDlpPath(),
                "-f", "bestvideo+bestaudio/best",
                "--merge-output-format", "mp4",
                "-o", template.toString(),
                pageUrl
        );
        ProcessRunner.Result r = ProcessRunner.run(cmd, 600);
        r.requireSuccess();
        return findSingleMedia(dir);
    }

    private Path findSingleMedia(Path dir) throws IOException {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(p -> {
                        String n = p.getFileName().toString().toLowerCase();
                        return n.endsWith(".mp4") || n.endsWith(".webm") || n.endsWith(".mkv");
                    })
                    .findFirst()
                    .orElseThrow(() -> new IOException("No video file found under " + dir));
        }
    }
}
