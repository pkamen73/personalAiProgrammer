package de.itsourcerer.aiideassistant.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    private ChatMessage message;
    private ModelConfig modelConfig;
    private Long modelConfigId;
    private String ollamaModel;
    private List<HistoryMessage> history;

    public List<HistoryMessage> getHistory() {
        return history != null ? history : Collections.emptyList();
    }
}
