package de.itsourcerer.aiideassistant.service;

import de.itsourcerer.aiideassistant.model.ProjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ProjectService {

    @Value("${workspace.root-path:./workspace}")
    private String workspaceRoot;

    public ProjectNode getProjectTree(String relativePath) {
        Path rootPath = Paths.get(workspaceRoot).toAbsolutePath().normalize();
        
        if (!Files.exists(rootPath)) {
            try {
                Files.createDirectories(rootPath);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create workspace directory", e);
            }
        }
        
        Path targetPath = relativePath != null ? rootPath.resolve(relativePath) : rootPath;
        
        return buildTree(targetPath.toFile(), rootPath.toFile());
    }

    private ProjectNode buildTree(File file, File workspaceRoot) {
        String relativePath = workspaceRoot.toPath().relativize(file.toPath()).toString();
        if (relativePath.isEmpty()) {
            relativePath = "/";
        } else {
            relativePath = "/" + relativePath.replace("\\", "/");
        }
        
        String displayName = file.getName();
        if (displayName.isEmpty()) {
            displayName = "workspace";
        }
        
        ProjectNode.ProjectNodeBuilder builder = ProjectNode.builder()
                .name(displayName)
                .path(relativePath)
                .type(file.isDirectory() ? ProjectNode.NodeType.DIRECTORY : ProjectNode.NodeType.FILE);

        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                List<ProjectNode> childNodes = new ArrayList<>();
                for (File child : children) {
                    if (!child.getName().startsWith(".") && !child.isHidden()) {
                        childNodes.add(buildTree(child, workspaceRoot));
                    }
                }
                childNodes.sort((a, b) -> {
                    if (a.getType() == b.getType()) {
                        return a.getName().compareToIgnoreCase(b.getName());
                    }
                    return a.getType() == ProjectNode.NodeType.DIRECTORY ? -1 : 1;
                });
                builder.children(childNodes);
            }
        }

        return builder.build();
    }

    public void createDirectory(String relativePath) {
        try {
            Path targetPath = Paths.get(workspaceRoot).resolve(relativePath);
            Files.createDirectories(targetPath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create directory: " + relativePath, e);
        }
    }

    public void delete(String relativePath) {
        try {
            Path targetPath = Paths.get(workspaceRoot).resolve(relativePath);
            if (Files.isDirectory(targetPath)) {
                try (Stream<Path> walk = Files.walk(targetPath)) {
                    walk.sorted((a, b) -> b.compareTo(a))
                            .forEach(path -> {
                                try {
                                    Files.delete(path);
                                } catch (Exception e) {
                                    throw new RuntimeException("Failed to delete: " + path, e);
                                }
                            });
                }
            } else {
                Files.delete(targetPath);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete: " + relativePath, e);
        }
    }
}
