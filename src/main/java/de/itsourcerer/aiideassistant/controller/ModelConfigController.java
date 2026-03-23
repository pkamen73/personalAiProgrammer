package de.itsourcerer.aiideassistant.controller;

import de.itsourcerer.aiideassistant.entity.ModelConfiguration;
import de.itsourcerer.aiideassistant.service.ModelConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/models/configs")
@RequiredArgsConstructor
public class ModelConfigController {

    private final ModelConfigService modelConfigService;

    @GetMapping
    public ResponseEntity<List<ModelConfiguration>> getAllConfigs() {
        return ResponseEntity.ok(modelConfigService.getAllConfigs());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ModelConfiguration> getConfig(@PathVariable Long id) {
        return modelConfigService.getConfigById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/default")
    public ResponseEntity<ModelConfiguration> getDefaultConfig() {
        return modelConfigService.getDefaultConfig()
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ModelConfiguration> createConfig(@RequestBody ModelConfiguration config) {
        return ResponseEntity.ok(modelConfigService.createConfig(config));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ModelConfiguration> updateConfig(
            @PathVariable Long id,
            @RequestBody ModelConfiguration config) {
        return ResponseEntity.ok(modelConfigService.updateConfig(id, config));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConfig(@PathVariable Long id) {
        modelConfigService.deleteConfig(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/set-default")
    public ResponseEntity<ModelConfiguration> setDefault(@PathVariable Long id) {
        return ResponseEntity.ok(modelConfigService.setDefault(id));
    }
}
