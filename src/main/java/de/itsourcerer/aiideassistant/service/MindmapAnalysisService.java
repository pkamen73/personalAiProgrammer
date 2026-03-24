package de.itsourcerer.aiideassistant.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MindmapAnalysisService {

    private final ImageStorageService imageStorageService;
    private final ModelConfigService modelConfigService;
    private final WebClient webClient = WebClient.builder().build();
    
    private static final String OLLAMA_CHAT_URL = "http://localhost:11434/api/chat";
    private static final String OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final String VISION_MODEL_LOCAL = "qwen3-vl:4b";
    private static final String PROMPT = "Analyze and summarize this mind map. Identify key concepts, relationships, and hierarchy. Output a PlantUML class diagram representing the structure.";

    public String analyzeMindmap(String imageId, String modelType) throws Exception {
        if ("cloud".equalsIgnoreCase(modelType)) {
            return analyzeMindmapCloud(imageId);
        } else {
            return analyzeMindmapLocal(imageId);
        }
    }

    private String analyzeMindmapLocal(String imageId) throws Exception {
        System.out.println("Loading image: " + imageId);
        String base64Image = imageStorageService.imageToBase64(imageId);
        System.out.println("✓ Image loaded, base64 length: " + base64Image.length());
        
        Map<String, Object> message = Map.of(
            "role", "user",
            "content", PROMPT,
            "images", List.of(base64Image)
        );
        
        Map<String, Object> requestBody = Map.of(
            "model", VISION_MODEL_LOCAL,
            "messages", List.of(message),
            "stream", false
        );
        
        System.out.println("Calling Ollama at: " + OLLAMA_CHAT_URL);
        System.out.println("Model: " + VISION_MODEL_LOCAL);
        
        try {
            Map response = webClient.post()
                .uri(OLLAMA_CHAT_URL)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(java.time.Duration.ofMinutes(5))
                .block();
            
            System.out.println("✓ Received response from Ollama");
            System.out.println("Response keys: " + (response != null ? response.keySet() : "null"));
            
            if (response != null && response.containsKey("message")) {
                Map messageMap = (Map) response.get("message");
                String content = (String) messageMap.get("content");
                System.out.println("✓ Extracted content, length: " + (content != null ? content.length() : "null"));
                return content;
            }
            
            System.err.println("✗ Response missing 'message' key");
            throw new RuntimeException("No response from vision model - invalid format");
        } catch (Exception e) {
            System.err.println("✗ Ollama call failed: " + e.getClass().getName() + " - " + e.getMessage());
            throw e;
        }
    }

    private String analyzeMindmapCloud(String imageId) throws Exception {
        System.out.println("Loading image for cloud analysis: " + imageId);
        String base64Image = imageStorageService.imageToBase64(imageId);
        System.out.println("✓ Image loaded, base64 length: " + base64Image.length());
        
        var openrouterConfig = modelConfigService.getAllConfigs().stream()
            .filter(c -> "openrouter".equalsIgnoreCase(c.getProvider()))
            .filter(c -> c.getModelName().contains("claude"))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No OpenRouter Claude config found"));
        
        System.out.println("Using cloud model: " + openrouterConfig.getModelName());
        
        Map<String, Object> message = Map.of(
            "role", "user",
            "content", List.of(
                Map.of("type", "text", "text", PROMPT),
                Map.of("type", "image_url", "image_url", Map.of("url", "data:image/jpeg;base64," + base64Image))
            )
        );
        
        Map<String, Object> requestBody = Map.of(
            "model", openrouterConfig.getModelName(),
            "messages", List.of(message)
        );
        
        System.out.println("Calling OpenRouter at: " + OPENROUTER_URL);
        
        try {
            Map response = webClient.post()
                .uri(OPENROUTER_URL)
                .header("Authorization", "Bearer " + openrouterConfig.getApiKey())
                .header("HTTP-Referer", "http://localhost:8080")
                .header("X-Title", "AI IDE Assistant")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(java.time.Duration.ofMinutes(5))
                .block();
            
            System.out.println("✓ Received response from OpenRouter");
            
            if (response != null && response.containsKey("choices")) {
                List choices = (List) response.get("choices");
                if (!choices.isEmpty()) {
                    Map choice = (Map) choices.get(0);
                    Map message2 = (Map) choice.get("message");
                    String content = (String) message2.get("content");
                    System.out.println("✓ Extracted content, length: " + (content != null ? content.length() : "null"));
                    return content;
                }
            }
            
            throw new RuntimeException("Invalid response from OpenRouter");
        } catch (Exception e) {
            System.err.println("✗ OpenRouter call failed: " + e.getMessage());
            throw e;
        }
    }
}
