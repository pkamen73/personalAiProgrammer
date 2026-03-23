package de.itsourcerer.aiideassistant.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    
    private String id;
    private MessageRole role;
    private String content;
    private Instant timestamp;
    private String conversationId;
    private List<FileContext> fileContext;
    
    public enum MessageRole {
        USER,
        ASSISTANT,
        SYSTEM
    }
}
