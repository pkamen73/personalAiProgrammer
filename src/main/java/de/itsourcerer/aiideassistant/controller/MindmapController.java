package de.itsourcerer.aiideassistant.controller;

import de.itsourcerer.aiideassistant.service.DiagramGenerationService;
import de.itsourcerer.aiideassistant.service.MindmapAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/mindmap")
@RequiredArgsConstructor
public class MindmapController {

    private final MindmapAnalysisService mindmapAnalysisService;
    private final DiagramGenerationService diagramGenerationService;
    
    @Value("${workspace.root-path:.}")
    private String workspaceRoot;

    @PostMapping("/analyze")
    public ResponseEntity<Map<String, String>> analyzeMindmap(@RequestBody Map<String, String> request) {
        try {
            System.out.println("=== Mindmap Analysis Request ===");
            String imageId = request.get("imageId");
            String modelType = request.getOrDefault("modelType", "local");
            Long promptId = request.containsKey("promptId") ? Long.parseLong(request.get("promptId")) : null;
            System.out.println("ImageId: " + imageId);
            System.out.println("Model type: " + modelType);
            System.out.println("Prompt ID: " + promptId);
            
            String analysis = mindmapAnalysisService.analyzeMindmap(imageId, modelType, promptId);
            System.out.println("✓ Analysis complete, length: " + analysis.length());
            
            return ResponseEntity.ok(Map.of("analysis", analysis));
        } catch (Exception e) {
            System.err.println("✗ Analysis failed: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/generate-diagram")
    public ResponseEntity<Map<String, String>> generateDiagram(@RequestBody Map<String, String> request) {
        try {
            System.out.println("=== Generate Diagram Request ===");
            String imageId = request.get("imageId");
            String analysisText = request.get("analysisText");
            System.out.println("ImageId: " + imageId);
            System.out.println("Analysis length: " + (analysisText != null ? analysisText.length() : "null"));
            
            String diagramId = diagramGenerationService.generateDiagram(analysisText, imageId);
            String analysisFileName = diagramId.replace(".png", ".txt");
            System.out.println("✓ Diagram generated: " + diagramId);
            System.out.println("✓ Analysis text saved: " + analysisFileName);
            
            return ResponseEntity.ok(Map.of(
                "diagramId", diagramId,
                "diagramUrl", "/api/images/" + diagramId,
                "analysisFileUrl", "/api/images/" + analysisFileName
            ));
        } catch (Exception e) {
            System.err.println("✗ Diagram generation failed: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/analyses")
    public ResponseEntity<List<Map<String, String>>> listAnalyses() {
        try {
            Path diagramDir = Paths.get(workspaceRoot).toAbsolutePath().resolve(".ai-ide/diagrams");
            
            if (!Files.exists(diagramDir)) {
                return ResponseEntity.ok(List.of());
            }
            
            List<Map<String, String>> analyses = Files.list(diagramDir)
                .filter(p -> p.toString().endsWith(".txt"))
                .map(p -> {
                    String filename = p.getFileName().toString();
                    String diagramFilename = filename.replace(".txt", ".png");
                    Path diagramPath = diagramDir.resolve(diagramFilename);
                    
                    try {
                        String modified = Files.getLastModifiedTime(p).toString();
                        boolean hasDiagram = Files.exists(diagramPath);
                        
                        return Map.of(
                            "filename", filename,
                            "modified", modified,
                            "diagramUrl", hasDiagram ? "/api/images/" + diagramFilename : "",
                            "hasDiagram", String.valueOf(hasDiagram)
                        );
                    } catch (Exception e) {
                        return Map.of("filename", filename, "hasDiagram", "false");
                    }
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(analyses);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/analysis/{filename}")
    public ResponseEntity<Map<String, String>> getAnalysis(@PathVariable String filename) {
        try {
            Path analysisPath = Paths.get(workspaceRoot).toAbsolutePath()
                .resolve(".ai-ide/diagrams")
                .resolve(filename);
            
            if (!Files.exists(analysisPath) || !filename.endsWith(".txt")) {
                return ResponseEntity.notFound().build();
            }
            
            String content = Files.readString(analysisPath);
            String associatedImage = filename.replace("diagram-", "").replace("-txt", ".jpeg");
            
            return ResponseEntity.ok(Map.of(
                "content", content,
                "filename", filename,
                "associatedImage", associatedImage
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/preview-diagram")
    public ResponseEntity<Map<String, String>> previewDiagram(@RequestBody Map<String, String> request) {
        try {
            String analysisText = request.get("analysisText");
            
            byte[] pngBytes = diagramGenerationService.generatePreview(analysisText);
            String base64Image = java.util.Base64.getEncoder().encodeToString(pngBytes);
            
            return ResponseEntity.ok(Map.of("image", "data:image/png;base64," + base64Image));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
        }
    }
}
