package de.itsourcerer.aiideassistant.service;

import de.itsourcerer.aiideassistant.entity.ModelConfiguration;
import de.itsourcerer.aiideassistant.repository.ModelConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ModelConfigService {

    private final ModelConfigRepository repository;

    public List<ModelConfiguration> getAllConfigs() {
        return repository.findAll();
    }

    public Optional<ModelConfiguration> getConfigById(Long id) {
        return repository.findById(id);
    }

    public Optional<ModelConfiguration> getDefaultConfig() {
        return repository.findByIsDefaultTrue();
    }

    @Transactional
    public ModelConfiguration createConfig(ModelConfiguration config) {
        if (config.getIsDefault()) {
            clearAllDefaults();
        }
        return repository.save(config);
    }

    @Transactional
    public ModelConfiguration updateConfig(Long id, ModelConfiguration config) {
        ModelConfiguration existing = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Config not found: " + id));
        
        existing.setProvider(config.getProvider());
        existing.setModelName(config.getModelName());
        existing.setDisplayName(config.getDisplayName());
        existing.setApiKey(config.getApiKey());
        existing.setType(config.getType());
        
        if (config.getIsDefault() && !existing.getIsDefault()) {
            clearAllDefaults();
            existing.setIsDefault(true);
        }
        
        return repository.save(existing);
    }

    @Transactional
    public void deleteConfig(Long id) {
        repository.deleteById(id);
    }

    @Transactional
    public ModelConfiguration setDefault(Long id) {
        clearAllDefaults();
        
        ModelConfiguration config = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Config not found: " + id));
        
        config.setIsDefault(true);
        return repository.save(config);
    }

    private void clearAllDefaults() {
        List<ModelConfiguration> allConfigs = repository.findAll();
        allConfigs.forEach(c -> c.setIsDefault(false));
        repository.saveAll(allConfigs);
    }
}
