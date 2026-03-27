package de.itsourcerer.aiideassistant.service.provider;

import de.itsourcerer.aiideassistant.model.HistoryMessage;
import de.itsourcerer.aiideassistant.model.ModelConfig;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class OpenRouterProvider implements ModelProvider {
    
    private static final String OPENROUTER_API_URL = "https://openrouter.ai/api/v1/chat/completions";
    private final WebClient webClient;
    
    public OpenRouterProvider() {
        this.webClient = WebClient.builder().build();
    }
    
    @Override
    public String generateResponse(String prompt, ModelConfig config) {
        StringBuilder response = new StringBuilder();
        streamResponse(prompt, config, Collections.emptyList())
            .doOnNext(response::append)
            .blockLast();
        return response.toString();
    }

    public Flux<String> streamResponse(String prompt, ModelConfig config, List<HistoryMessage> history) {
        List<Map<String, String>> messages = new ArrayList<>();
        for (HistoryMessage h : history) {
            messages.add(Map.of("role", h.getRole(), "content", h.getContent()));
        }
        messages.add(Map.of("role", "user", "content", prompt));

        Map<String, Object> requestBody = Map.of(
            "model", config.getModelName(),
            "messages", messages,
            "stream", true
        );
        
        return webClient.post()
            .uri(OPENROUTER_API_URL)
            .header("Authorization", "Bearer " + config.getApiKey())
            .header("HTTP-Referer", "http://localhost:8080")
            .header("X-Title", "AI IDE Assistant")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToFlux(String.class)
            .mapNotNull(line -> {
                if (line.startsWith("data: ") && !line.contains("[DONE]")) {
                    try {
                        String json = line.substring(6);
                        return extractContent(json);
                    } catch (Exception e) {
                        return null;
                    }
                }
                return null;
            });
    }
    
    private String extractContent(String json) {
        int contentStart = json.indexOf("\"content\":\"");
        if (contentStart == -1) return null;
        contentStart += 11;
        
        int contentEnd = json.indexOf("\"", contentStart);
        if (contentEnd == -1) return null;
        
        return json.substring(contentStart, contentEnd)
            .replace("\\n", "\n")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\");
    }
}
