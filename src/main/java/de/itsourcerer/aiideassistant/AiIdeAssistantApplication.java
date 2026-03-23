package de.itsourcerer.aiideassistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
public class AiIdeAssistantApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(AiIdeAssistantApplication.class);
        
        if (args.length > 0) {
            String workspacePath = args[0];
            
            if (".".equals(workspacePath)) {
                workspacePath = System.getProperty("user.dir");
            }
            
            Path resolvedPath = Paths.get(workspacePath).toAbsolutePath().normalize();
            
            if (!Files.exists(resolvedPath)) {
                System.err.println("Error: Workspace path does not exist: " + resolvedPath);
                System.err.println("Creating workspace directory...");
                try {
                    Files.createDirectories(resolvedPath);
                    System.out.println("Created workspace directory: " + resolvedPath);
                } catch (Exception e) {
                    System.err.println("Failed to create workspace directory: " + e.getMessage());
                    System.exit(1);
                }
            }
            
            if (!Files.isDirectory(resolvedPath)) {
                System.err.println("Error: Workspace path is not a directory: " + resolvedPath);
                System.exit(1);
            }
            
            System.out.println("Using workspace directory: " + resolvedPath);
            app.setDefaultProperties(java.util.Map.of("workspace.root-path", resolvedPath.toString()));
        }
        
        app.run(args);
    }
}
