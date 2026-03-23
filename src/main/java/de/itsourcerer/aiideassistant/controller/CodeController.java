package de.itsourcerer.aiideassistant.controller;

import de.itsourcerer.aiideassistant.model.FileChange;
import de.itsourcerer.aiideassistant.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/code")
@RequiredArgsConstructor
public class CodeController {

    private final FileService fileService;

    @PostMapping("/apply")
    public ResponseEntity<Void> applyChanges(@RequestBody List<FileChange> changes) {
        System.out.println("=== CodeController.applyChanges ===");
        System.out.println("Received " + changes.size() + " changes");
        
        for (FileChange change : changes) {
            System.out.println("Processing: " + change.getPath() + " (" + change.getChangeType() + ")");
            
            switch (change.getChangeType()) {
                case CREATE, MODIFY -> {
                    de.itsourcerer.aiideassistant.model.FileContent fileContent = 
                        de.itsourcerer.aiideassistant.model.FileContent.builder()
                            .path(change.getPath())
                            .content(change.getNewContent())
                            .build();
                    fileService.writeFile(fileContent);
                    System.out.println("✓ Written: " + change.getPath());
                }
                case DELETE -> {
                    fileService.deleteFile(change.getPath());
                    System.out.println("✓ Deleted: " + change.getPath());
                }
            }
        }
        return ResponseEntity.ok().build();
    }
}
