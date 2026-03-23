package de.itsourcerer.aiideassistant.service;

import de.itsourcerer.aiideassistant.entity.ModelConfiguration;
import de.itsourcerer.aiideassistant.model.ChatMessage;
import de.itsourcerer.aiideassistant.model.FileContext;
import de.itsourcerer.aiideassistant.model.ModelConfig;
import de.itsourcerer.aiideassistant.service.provider.ModelProvider;
import de.itsourcerer.aiideassistant.service.provider.OllamaProvider;
import de.itsourcerer.aiideassistant.service.provider.OpenRouterProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ModelProviderService modelProviderService;
    private final ModelConfigService modelConfigService;
    private final SimpMessagingTemplate messagingTemplate;

    public void processMessageStreaming(ChatMessage userMessage, Long modelConfigId, String ollamaModel) {
        System.out.println("=== ChatService.processMessageStreaming ===");
        System.out.println("modelConfigId: " + modelConfigId);
        System.out.println("ollamaModel: " + ollamaModel);
        
        String messageId = UUID.randomUUID().toString();
        String promptWithContext = buildPromptWithFileContext(userMessage);
        
        if (ollamaModel != null && !ollamaModel.isEmpty()) {
            System.out.println("Using Ollama path");
            handleOllamaStreaming(messageId, promptWithContext, ollamaModel, userMessage.getConversationId());
            return;
        }
        
        System.out.println("Using cloud model path");
        
        ModelConfiguration dbConfig = modelConfigId != null 
            ? modelConfigService.getConfigById(modelConfigId).orElseThrow(() -> new RuntimeException("Model config not found"))
            : modelConfigService.getDefaultConfig().orElseThrow(() -> new RuntimeException("No default model config"));
        
        String provider = dbConfig.getProvider().toLowerCase();
        ModelProvider modelProvider = modelProviderService.getProvider(provider);
        
        if (modelProvider == null) {
            sendErrorMessage(userMessage, "Unknown provider: " + provider);
            return;
        }
        
        ModelConfig.ModelType configType = dbConfig.getType() == de.itsourcerer.aiideassistant.entity.ModelConfiguration.ModelType.CLOUD 
            ? ModelConfig.ModelType.CLOUD 
            : ModelConfig.ModelType.LOCAL;
        
        ModelConfig config = ModelConfig.builder()
            .provider(dbConfig.getProvider())
            .modelName(dbConfig.getModelName())
            .apiKey(dbConfig.getApiKey())
            .type(configType)
            .build();
        
        if ("openrouter".equals(provider)) {
            OpenRouterProvider openRouterProvider = (OpenRouterProvider) modelProvider;
            openRouterProvider.streamResponse(promptWithContext, config)
                .subscribe(
                    token -> sendToken(messageId, token, userMessage.getConversationId()),
                    error -> sendErrorMessage(userMessage, error.getMessage())
                );
        } else {
            String response = modelProviderService.generateResponse(promptWithContext, config);
            sendCompleteMessage(messageId, response, userMessage.getConversationId());
        }
    }
    
    private void handleOllamaStreaming(String messageId, String prompt, String modelName, String conversationId) {
        OllamaProvider ollamaProvider = (OllamaProvider) modelProviderService.getProvider("ollama");
        ollamaProvider.streamResponse(prompt, modelName)
            .subscribe(
                token -> sendToken(messageId, token, conversationId),
                error -> {
                    ChatMessage errorMsg = ChatMessage.builder()
                        .id(UUID.randomUUID().toString())
                        .role(ChatMessage.MessageRole.SYSTEM)
                        .content("Error: " + error.getMessage())
                        .timestamp(Instant.now())
                        .conversationId(conversationId)
                        .build();
                    messagingTemplate.convertAndSend("/topic/messages", errorMsg);
                }
            );
    }
    
    private void sendToken(String messageId, String token, String conversationId) {
        ChatMessage chunk = ChatMessage.builder()
            .id(messageId)
            .role(ChatMessage.MessageRole.ASSISTANT)
            .content(token)
            .timestamp(Instant.now())
            .conversationId(conversationId)
            .build();
        messagingTemplate.convertAndSend("/topic/messages", chunk);
    }
    
    private void sendCompleteMessage(String messageId, String content, String conversationId) {
        ChatMessage message = ChatMessage.builder()
            .id(messageId)
            .role(ChatMessage.MessageRole.ASSISTANT)
            .content(content)
            .timestamp(Instant.now())
            .conversationId(conversationId)
            .build();
        messagingTemplate.convertAndSend("/topic/messages", message);
    }
    
    private void sendErrorMessage(ChatMessage userMessage, String errorText) {
        ChatMessage errorMsg = ChatMessage.builder()
            .id(UUID.randomUUID().toString())
            .role(ChatMessage.MessageRole.SYSTEM)
            .content("Error: " + errorText)
            .timestamp(Instant.now())
            .conversationId(userMessage.getConversationId())
            .build();
        messagingTemplate.convertAndSend("/topic/messages", errorMsg);
    }
    
    private String buildPromptWithFileContext(ChatMessage message) {
        StringBuilder prompt = new StringBuilder();
        
        if (message.getFileContext() != null && !message.getFileContext().isEmpty()) {
            prompt.append("Here are the files for context:\n\n");
            
            for (FileContext file : message.getFileContext()) {
                prompt.append("File: ").append(file.getPath()).append("\n");
                prompt.append("```").append(file.getLanguage() != null ? file.getLanguage() : "").append("\n");
                prompt.append(file.getContent()).append("\n");
                prompt.append("```\n\n");
            }
            
            prompt.append("User question: ");
        }
        
        prompt.append(message.getContent());
        return prompt.toString();
    }
}
