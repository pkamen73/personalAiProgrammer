package de.itsourcerer.aiideassistant.controller;

import de.itsourcerer.aiideassistant.model.ChatRequest;
import de.itsourcerer.aiideassistant.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @MessageMapping("/chat")
    public void handleChatMessage(ChatRequest request) {
        chatService.processMessageStreaming(
            request.getMessage(),
            request.getModelConfigId(),
            request.getOllamaModel(),
            request.getHistory()
        );
    }
}
