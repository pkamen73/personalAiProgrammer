package de.itsourcerer.aiideassistant.model;

import lombok.Data;

@Data
public class TerminalSession {
    private String sessionId;
    private StringBuilder commandBuffer;
    
    public TerminalSession(String sessionId) {
        this.sessionId = sessionId;
        this.commandBuffer = new StringBuilder();
    }
}
