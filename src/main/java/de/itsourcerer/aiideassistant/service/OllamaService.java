package de.itsourcerer.aiideassistant.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class OllamaService {

    public List<String> getAvailableModels() {
        List<String> models = new ArrayList<>();
        try {
            Process process = Runtime.getRuntime().exec("ollama list");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }
                
                String[] parts = line.trim().split("\\s+");
                if (parts.length > 0 && !parts[0].isEmpty()) {
                    models.add(parts[0]);
                }
            }
            
            process.waitFor();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get Ollama models", e);
        }
        
        return models;
    }
}
