package de.itsourcerer.aiideassistant.controller;

import de.itsourcerer.aiideassistant.model.JavaFileInfo;
import de.itsourcerer.aiideassistant.model.ProjectNode;
import de.itsourcerer.aiideassistant.service.ProjectAnalyzerService;
import de.itsourcerer.aiideassistant.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/project")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectAnalyzerService projectAnalyzerService;

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

    @GetMapping("/analyze")
    public ResponseEntity<List<JavaFileInfo>> analyzeProject() {
        List<JavaFileInfo> analysis = projectAnalyzerService.analyzeProject();
        return ResponseEntity.ok(analysis);
    }
}
