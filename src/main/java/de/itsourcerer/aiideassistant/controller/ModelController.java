package de.itsourcerer.aiideassistant.controller;

import de.itsourcerer.aiideassistant.service.OllamaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/models")
@RequiredArgsConstructor
public class ModelController {

    private final OllamaService ollamaService;

    @GetMapping("/ollama")
    public ResponseEntity<List<String>> getOllamaModels() {
        List<String> models = ollamaService.getAvailableModels();
        return ResponseEntity.ok(models);
    }
}
