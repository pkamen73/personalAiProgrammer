package de.itsourcerer.aiideassistant.service;

import de.itsourcerer.aiideassistant.config.WorkspaceHolder;
import de.itsourcerer.aiideassistant.model.TerminalSession;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TerminalService {

    private final Map<String, TerminalSession> sessions = new ConcurrentHashMap<>();
    private final SimpMessagingTemplate messagingTemplate;
    private final WorkspaceHolder workspaceHolder;

    public TerminalService(SimpMessagingTemplate messagingTemplate, WorkspaceHolder workspaceHolder) {
        this.messagingTemplate = messagingTemplate;
        this.workspaceHolder = workspaceHolder;
    }

    public String createSession() {
        String sessionId = UUID.randomUUID().toString();
        System.out.println("=== Creating Simple Terminal Session ===");
        System.out.println("SessionId: " + sessionId);
        
        TerminalSession session = new TerminalSession(sessionId);
        sessions.put(sessionId, session);
        
        String prompt = workspaceHolder.getRootPath() + " $ ";
        messagingTemplate.convertAndSend("/topic/terminal/" + sessionId, prompt);
        System.out.println("✓ Sent initial prompt");
        
        return sessionId;
    }

    public void sendInput(String sessionId, String input) {
        System.out.println("Terminal input: sessionId=" + sessionId + ", input=[" + input.replace("\r", "\\r").replace("\n", "\\n") + "]");
        
        TerminalSession session = sessions.get(sessionId);
        if (session == null) {
            System.err.println("Session not found: " + sessionId);
            return;
        }
        
        if (input.equals("\r")) {
            String command = session.getCommandBuffer().toString().trim();
            session.getCommandBuffer().setLength(0);
            
            if (!command.isEmpty()) {
                System.out.println("Executing: [" + command + "]");
                executeCommand(sessionId, command);
            } else {
                messagingTemplate.convertAndSend("/topic/terminal/" + sessionId, "\r\n" + workspaceHolder.getRootPath() + " $ ");
            }
        } else if (input.equals("\u007f") || input.equals("\b")) {
            if (session.getCommandBuffer().length() > 0) {
                session.getCommandBuffer().setLength(session.getCommandBuffer().length() - 1);
                messagingTemplate.convertAndSend("/topic/terminal/" + sessionId, "\b \b");
            }
        } else if (!input.equals("\n")) {
            session.getCommandBuffer().append(input);
            messagingTemplate.convertAndSend("/topic/terminal/" + sessionId, input);
        }
    }
    
    private void executeCommand(String sessionId, String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", command);
            pb.directory(new File(workspaceHolder.getRootPath()));
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            InputStream in = process.getInputStream();
            byte[] buffer = new byte[8192];
            int read;
            StringBuilder output = new StringBuilder("\r\n");
            
            while ((read = in.read(buffer)) != -1) {
                String chunk = new String(buffer, 0, read, StandardCharsets.UTF_8);
                output.append(chunk.replace("\n", "\r\n"));
            }
            
            process.waitFor();
            
            if (!output.toString().endsWith("\r\n")) {
                output.append("\r\n");
            }
            output.append(workspaceHolder.getRootPath()).append(" $ ");
            
            messagingTemplate.convertAndSend("/topic/terminal/" + sessionId, output.toString());
            System.out.println("✓ Command completed");
            
        } catch (Exception e) {
            System.err.println("Command execution error: " + e.getMessage());
            messagingTemplate.convertAndSend("/topic/terminal/" + sessionId, 
                "\r\nError: " + e.getMessage() + "\r\n" + workspaceHolder.getRootPath() + " $ ");
        }
    }

    public void resize(String sessionId, int cols, int rows) {
    }

    public void closeSession(String sessionId) {
        sessions.remove(sessionId);
    }
}
