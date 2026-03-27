package de.itsourcerer.aiideassistant.service;

import de.itsourcerer.aiideassistant.config.WorkspaceHolder;
import de.itsourcerer.aiideassistant.model.FileContent;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class FileService {

    private final WorkspaceHolder workspaceHolder;

    public FileContent readFile(String relativePath) {
        try {
            String cleanPath = relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;
            Path targetPath = Paths.get(workspaceHolder.getRootPath()).resolve(cleanPath);
            String content = FileUtils.readFileToString(targetPath.toFile(), StandardCharsets.UTF_8);
            
            return FileContent.builder()
                    .path(relativePath)
                    .content(content)
                    .language(detectLanguage(relativePath))
                    .lastModified(Files.getLastModifiedTime(targetPath).toMillis())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read file: " + relativePath, e);
        }
    }

    public void writeFile(FileContent fileContent) {
        try {
            String cleanPath = fileContent.getPath().startsWith("/") ? fileContent.getPath().substring(1) : fileContent.getPath();
            Path targetPath = Paths.get(workspaceHolder.getRootPath()).resolve(cleanPath);
            Files.createDirectories(targetPath.getParent());
            FileUtils.writeStringToFile(targetPath.toFile(), fileContent.getContent(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write file: " + fileContent.getPath(), e);
        }
    }

    public void deleteFile(String relativePath) {
        try {
            String cleanPath = relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;
            Path targetPath = Paths.get(workspaceHolder.getRootPath()).resolve(cleanPath);
            Files.delete(targetPath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete file: " + relativePath, e);
        }
    }

    private String detectLanguage(String filePath) {
        String extension = filePath.substring(filePath.lastIndexOf('.') + 1);
        return switch (extension.toLowerCase()) {
            case "java" -> "java";
            case "js" -> "javascript";
            case "ts" -> "typescript";
            case "jsx" -> "javascriptreact";
            case "tsx" -> "typescriptreact";
            case "py" -> "python";
            case "json" -> "json";
            case "xml" -> "xml";
            case "yml", "yaml" -> "yaml";
            case "md" -> "markdown";
            case "html" -> "html";
            case "css" -> "css";
            default -> "plaintext";
        };
    }
}
