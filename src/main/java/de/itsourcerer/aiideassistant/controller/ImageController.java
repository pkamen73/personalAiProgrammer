package de.itsourcerer.aiideassistant.controller;

import de.itsourcerer.aiideassistant.service.DiagramGenerationService;
import de.itsourcerer.aiideassistant.service.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageStorageService imageStorageService;
    private final DiagramGenerationService diagramGenerationService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            System.out.println("=== Image Upload Request ===");
            System.out.println("Filename: " + file.getOriginalFilename());
            System.out.println("Size: " + file.getSize() + " bytes");
            System.out.println("Content-Type: " + file.getContentType());
            
            String imageId = imageStorageService.saveImage(file);
            System.out.println("✓ Saved as: " + imageId);
            
            return ResponseEntity.ok(Map.of("imageId", imageId, "url", "/api/images/" + imageId));
        } catch (Exception e) {
            System.err.println("✗ Upload failed: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{imageId}")
    public ResponseEntity<byte[]> getImage(@PathVariable String imageId) {
        try {
            byte[] image;
            try {
                image = diagramGenerationService.getDiagram(imageId);
            } catch (Exception e) {
                image = imageStorageService.getImage(imageId);
            }
            String contentType = imageId.endsWith(".txt") ? "text/plain" : "image/png";
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(image);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
