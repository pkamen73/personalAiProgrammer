package de.itsourcerer.aiideassistant.service;

import de.itsourcerer.aiideassistant.config.WorkspaceHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageStorageService {

    private final WorkspaceHolder workspaceHolder;

    private static final String IMAGES_DIR = ".ai-ide/mindmaps";

    public String saveImage(MultipartFile file) throws IOException {
        String imageId = UUID.randomUUID().toString() + getExtension(file.getOriginalFilename());
        Path imageDir = Paths.get(workspaceHolder.getRootPath()).toAbsolutePath().resolve(IMAGES_DIR);
        
        System.out.println("Creating directory: " + imageDir);
        Files.createDirectories(imageDir);
        
        Path imagePath = imageDir.resolve(imageId).toAbsolutePath();
        System.out.println("Saving to: " + imagePath);
        
        Files.write(imagePath, file.getBytes());
        System.out.println("✓ File written successfully");
        
        return imageId;
    }

    public byte[] getImage(String imageId) throws IOException {
        Path imagePath = Paths.get(workspaceHolder.getRootPath()).toAbsolutePath().resolve(IMAGES_DIR).resolve(imageId);
        if (!Files.exists(imagePath)) {
            throw new IOException("Image not found: " + imageId);
        }
        return Files.readAllBytes(imagePath);
    }

    public String getImagePath(String imageId) {
        return Paths.get(workspaceHolder.getRootPath()).toAbsolutePath().resolve(IMAGES_DIR).resolve(imageId).toString();
    }

    public String imageToBase64(String imageId) throws IOException {
        byte[] imageBytes = getImage(imageId);
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    private String getExtension(String filename) {
        if (filename == null) return ".png";
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(dotIndex) : ".png";
    }
}
