package de.itsourcerer.aiideassistant.service.provider;

import de.itsourcerer.aiideassistant.model.ModelConfig;

public interface ModelProvider {
    String generateResponse(String prompt, ModelConfig config);
}
