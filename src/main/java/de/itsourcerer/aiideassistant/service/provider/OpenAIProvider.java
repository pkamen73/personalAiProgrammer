package de.itsourcerer.aiideassistant.service.provider;

import de.itsourcerer.aiideassistant.model.ModelConfig;

public class OpenAIProvider implements ModelProvider {
    
    @Override
    public String generateResponse(String prompt, ModelConfig config) {
        return "OpenAI response placeholder for: " + prompt;
    }
}
