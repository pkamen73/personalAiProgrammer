package de.itsourcerer.aiideassistant.controller;

import de.itsourcerer.aiideassistant.config.WorkspaceHolder;
import de.itsourcerer.aiideassistant.model.JavaFileInfo;
import de.itsourcerer.aiideassistant.model.ProjectAnalysisResult;
import de.itsourcerer.aiideassistant.model.ProjectNode;
import de.itsourcerer.aiideassistant.service.ProjectAnalysisPipelineService;
import de.itsourcerer.aiideassistant.service.ProjectAnalyzerService;
import de.itsourcerer.aiideassistant.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/project")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectAnalyzerService projectAnalyzerService;
    private final ProjectAnalysisPipelineService pipelineService;
    private final WorkspaceHolder workspaceHolder;

    @PostMapping("/open")
    public ResponseEntity<Map<String, String>> openProject(@RequestParam String path) {
        try {
            java.nio.file.Path target = java.nio.file.Paths.get(path).toAbsolutePath().normalize();
            if (!java.nio.file.Files.isDirectory(target)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Path is not a directory: " + path));
            }
            workspaceHolder.setRootPath(target.toString());
            return ResponseEntity.ok(Map.of(
                    "path", workspaceHolder.getAbsolutePath(),
                    "message", "Project opened: " + target.getFileName()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/tree")
    public ResponseEntity<ProjectNode> getProjectTree(@RequestParam(required = false) String path) {
        ProjectNode tree = projectService.getProjectTree(path);
        return ResponseEntity.ok(tree);
    }

    @PostMapping("/directory")
    public ResponseEntity<Void> createDirectory(@RequestParam String path) {
        projectService.createDirectory(path);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@RequestParam String path) {
        projectService.delete(path);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/root")
    public ResponseEntity<Map<String, String>> getWorkspaceRoot() {
        return ResponseEntity.ok(Map.of("path", projectService.getWorkspaceRoot()));
    }

    @GetMapping("/analyze")
    public ResponseEntity<List<JavaFileInfo>> analyzeProject() {
        List<JavaFileInfo> analysis = projectAnalyzerService.analyzeProject();
        return ResponseEntity.ok(analysis);
    }

    @PostMapping("/analyze-full")
    public ResponseEntity<Map<String, String>> analyzeFullAsync(
            @RequestParam(required = false) String ollamaModel,
            @RequestParam(required = false) String projectPath) {
        pipelineService.runAsync(ollamaModel, projectPath);
        return ResponseEntity.accepted().body(Map.of(
                "status", "RUNNING",
                "message", "Analysis pipeline started. Docs will be written to .ai-ide/docs/"
        ));
    }

    @PostMapping("/analyze-full/sync")
    public ResponseEntity<ProjectAnalysisResult> analyzeFullSync(
            @RequestParam(required = false) String ollamaModel,
            @RequestParam(required = false) String projectPath) {
        ProjectAnalysisResult result = pipelineService.run(ollamaModel, projectPath);
        return ResponseEntity.ok(result);
    }
}
