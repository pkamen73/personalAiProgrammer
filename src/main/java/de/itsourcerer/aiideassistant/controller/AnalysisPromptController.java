package de.itsourcerer.aiideassistant.controller;

import de.itsourcerer.aiideassistant.entity.AnalysisPrompt;
import de.itsourcerer.aiideassistant.service.AnalysisPromptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prompts")
@RequiredArgsConstructor
public class AnalysisPromptController {

    private final AnalysisPromptService promptService;

    @GetMapping
    public ResponseEntity<List<AnalysisPrompt>> getAllPrompts() {
        return ResponseEntity.ok(promptService.getAllPrompts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AnalysisPrompt> getPrompt(@PathVariable Long id) {
        return promptService.getPromptById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/default")
    public ResponseEntity<AnalysisPrompt> getDefaultPrompt() {
        return promptService.getDefaultPrompt()
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<AnalysisPrompt> createPrompt(@RequestBody AnalysisPrompt prompt) {
        return ResponseEntity.ok(promptService.createPrompt(prompt));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AnalysisPrompt> updatePrompt(
            @PathVariable Long id,
            @RequestBody AnalysisPrompt prompt) {
        try {
            return ResponseEntity.ok(promptService.updatePrompt(id, prompt));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePrompt(@PathVariable Long id) {
        try {
            promptService.deletePrompt(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/set-default")
    public ResponseEntity<AnalysisPrompt> setDefault(@PathVariable Long id) {
        return ResponseEntity.ok(promptService.setDefault(id));
    }

    @PostMapping("/reinitialize-defaults")
    public ResponseEntity<String> reinitializeDefaults() {
        promptService.reinitializeDefaults();
        return ResponseEntity.ok("Default prompts reinitialized");
    }
}
