package de.itsourcerer.aiideassistant.service;

import de.itsourcerer.aiideassistant.model.ModelConfig;
import de.itsourcerer.aiideassistant.service.provider.ModelProvider;
import de.itsourcerer.aiideassistant.service.provider.OpenAIProvider;
import de.itsourcerer.aiideassistant.service.provider.AnthropicProvider;
import de.itsourcerer.aiideassistant.service.provider.OllamaProvider;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Service
public class ModelProviderService {

    private final Map<String, ModelProvider> providers = new HashMap<>();

    @PostConstruct
    public void init() {
        providers.put("openai", new OpenAIProvider());
        providers.put("anthropic", new AnthropicProvider());
        providers.put("ollama", new OllamaProvider());
        providers.put("openrouter", new de.itsourcerer.aiideassistant.service.provider.OpenRouterProvider());
    }

    public String generateResponse(String prompt, ModelConfig config) {
        ModelProvider provider = providers.get(config.getProvider().toLowerCase());
        if (provider == null) {
            throw new IllegalArgumentException("Unknown provider: " + config.getProvider());
        }
        return provider.generateResponse(prompt, config);
    }
    
    public ModelProvider getProvider(String providerName) {
        return providers.get(providerName.toLowerCase());
    }
}
