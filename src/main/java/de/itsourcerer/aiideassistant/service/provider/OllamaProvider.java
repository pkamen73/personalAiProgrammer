package de.itsourcerer.aiideassistant.service.provider;

import de.itsourcerer.aiideassistant.model.HistoryMessage;
import de.itsourcerer.aiideassistant.model.ModelConfig;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class OllamaProvider implements ModelProvider {
    
    private static final String OLLAMA_CHAT_URL = "http://localhost:11434/api/chat";
    private final WebClient webClient;
    
    public OllamaProvider() {
        this.webClient = WebClient.builder().build();
    }
    
    @Override
    public String generateResponse(String prompt, ModelConfig config) {
        String model = config.getModelName() != null ? config.getModelName() : "llama2";
        StringBuilder response = new StringBuilder();
        streamResponse(prompt, model, Collections.emptyList())
            .doOnNext(response::append)
            .blockLast();
        return response.toString();
    }
    
    public Flux<String> streamResponse(String prompt, String model, List<HistoryMessage> history) {
        List<Map<String, String>> messages = new ArrayList<>();
        for (HistoryMessage h : history) {
            messages.add(Map.of("role", h.getRole(), "content", h.getContent()));
        }
        messages.add(Map.of("role", "user", "content", prompt));

        Map<String, Object> requestBody = Map.of(
            "model", model,
            "messages", messages,
            "stream", true
        );
        
        return webClient.post()
            .uri(OLLAMA_CHAT_URL)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToFlux(Map.class)
            .filter(map -> map.containsKey("message") && Boolean.FALSE.equals(map.get("done")))
            .map(map -> {
                Object msg = map.get("message");
                if (msg instanceof Map) {
                    Object content = ((Map<?, ?>) msg).get("content");
                    return content != null ? (String) content : "";
                }
                return "";
            })
            .filter(token -> !token.isEmpty());
    }
}
