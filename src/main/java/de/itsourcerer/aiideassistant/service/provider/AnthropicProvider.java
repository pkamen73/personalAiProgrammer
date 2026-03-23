package de.itsourcerer.aiideassistant.service.provider;

import de.itsourcerer.aiideassistant.model.ModelConfig;

public class AnthropicProvider implements ModelProvider {
    
    @Override
    public String generateResponse(String prompt, ModelConfig config) {
        return "Anthropic response placeholder for: " + prompt;
    }
}
