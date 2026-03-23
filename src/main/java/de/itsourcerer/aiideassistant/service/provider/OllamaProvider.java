package de.itsourcerer.aiideassistant.service.provider;

import de.itsourcerer.aiideassistant.model.ModelConfig;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.Map;

public class OllamaProvider implements ModelProvider {
    
    private static final String OLLAMA_API_URL = "http://localhost:11434/api/generate";
    private final WebClient webClient;
    
    public OllamaProvider() {
        this.webClient = WebClient.builder().build();
    }
    
    @Override
    public String generateResponse(String prompt, ModelConfig config) {
        String model = config.getModelName() != null ? config.getModelName() : "llama2";
        
        StringBuilder response = new StringBuilder();
        
        streamResponse(prompt, model)
            .doOnNext(token -> response.append(token))
            .blockLast();
        
        return response.toString();
    }
    
    public Flux<String> streamResponse(String prompt, String model) {
        Map<String, Object> requestBody = Map.of(
            "model", model,
            "prompt", prompt,
            "stream", true
        );
        
        return webClient.post()
            .uri(OLLAMA_API_URL)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToFlux(Map.class)
            .filter(map -> map.containsKey("response"))
            .map(map -> (String) map.get("response"));
    }
}
