package de.itsourcerer.aiideassistant.controller;

import de.itsourcerer.aiideassistant.config.WorkspaceHolder;
import de.itsourcerer.aiideassistant.model.FileContent;
import de.itsourcerer.aiideassistant.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final WorkspaceHolder workspaceHolder;

    private static final Map<String, String> IMAGE_TYPES = Map.of(
            "png", "image/png",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "gif", "image/gif",
            "svg", "image/svg+xml",
            "webp", "image/webp",
            "bmp", "image/bmp",
            "ico", "image/x-icon"
    );

    @GetMapping("/image")
    public ResponseEntity<byte[]> getImage(@RequestParam String path) {
        try {
            String clean = path.startsWith("/") ? path.substring(1) : path;
            Path target = Paths.get(workspaceHolder.getRootPath()).resolve(clean).toAbsolutePath().normalize();
            if (!target.startsWith(Paths.get(workspaceHolder.getRootPath()).toAbsolutePath().normalize())) {
                return ResponseEntity.badRequest().build();
            }
            byte[] bytes = Files.readAllBytes(target);
            String ext = path.substring(path.lastIndexOf('.') + 1).toLowerCase();
            String mime = IMAGE_TYPES.getOrDefault(ext, "application/octet-stream");
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(mime))
                    .body(bytes);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public ResponseEntity<FileContent> getFile(@RequestParam String path) {
        FileContent content = fileService.readFile(path);
        return ResponseEntity.ok(content);
    }

    @PostMapping
    public ResponseEntity<Void> saveFile(@RequestBody FileContent fileContent) {
        fileService.writeFile(fileContent);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteFile(@RequestParam String path) {
        fileService.deleteFile(path);
        return ResponseEntity.ok().build();
    }
}
