package de.itsourcerer.aiideassistant.controller;

import de.itsourcerer.aiideassistant.model.FileContent;
import de.itsourcerer.aiideassistant.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

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
