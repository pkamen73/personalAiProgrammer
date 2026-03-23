package de.itsourcerer.aiideassistant.controller;

import de.itsourcerer.aiideassistant.service.TerminalService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class TerminalController {

    private final TerminalService terminalService;

    @MessageMapping("/terminal/create")
    @SendTo("/topic/terminal/session")
    public Map<String, String> createTerminal(Map<String, Object> request) {
        System.out.println("=== Terminal Create Request Received ===");
        System.out.println("Request: " + request);
        try {
            String sessionId = terminalService.createSession();
            System.out.println("✓ Created session: " + sessionId);
            Map<String, String> response = Map.of("sessionId", sessionId);
            System.out.println("Sending response: " + response);
            return response;
        } catch (Exception e) {
            System.err.println("✗ Failed to create terminal: " + e.getMessage());
            e.printStackTrace();
            return Map.of("error", e.getMessage());
        }
    }

    @MessageMapping("/terminal/{sessionId}/input")
    public void sendInput(@DestinationVariable String sessionId, String input) throws IOException {
        System.out.println("Terminal input for " + sessionId + ": [" + input.replace("\r", "\\r").replace("\n", "\\n") + "]");
        terminalService.sendInput(sessionId, input);
    }

    @MessageMapping("/terminal/{sessionId}/resize")
    public void resize(@DestinationVariable String sessionId, Map<String, Integer> size) {
        terminalService.resize(sessionId, size.get("cols"), size.get("rows"));
    }

    @MessageMapping("/terminal/{sessionId}/close")
    public void closeTerminal(@DestinationVariable String sessionId) {
        terminalService.closeSession(sessionId);
    }
}
