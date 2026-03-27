package de.itsourcerer.aiideassistant.service;

import de.itsourcerer.aiideassistant.config.WorkspaceHolder;
import de.itsourcerer.aiideassistant.model.ProjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final WorkspaceHolder workspaceHolder;

    public String getWorkspaceRoot() {
        return workspaceHolder.getAbsolutePath();
    }

    public ProjectNode getProjectTree(String relativePath) {
        Path rootPath = Paths.get(workspaceHolder.getRootPath()).toAbsolutePath().normalize();
        
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
                    childNodes.add(buildTree(child, workspaceRoot));
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
            Path targetPath = Paths.get(workspaceHolder.getRootPath()).resolve(relativePath);
            Files.createDirectories(targetPath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create directory: " + relativePath, e);
        }
    }

    public void createFile(String relativePath) {
        try {
            Path target = resolve(relativePath);
            Files.createDirectories(target.getParent());
            Files.createFile(target);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create file: " + relativePath, e);
        }
    }

    public void rename(String relativePath, String newName) {
        try {
            Path source = resolve(relativePath);
            Path target = source.getParent().resolve(newName);
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            throw new RuntimeException("Failed to rename: " + relativePath, e);
        }
    }

    public void move(String sourcePath, String destPath) {
        try {
            Path source = resolve(sourcePath);
            Path dest = resolve(destPath);
            if (Files.isDirectory(source)) {
                dest = dest.resolve(source.getFileName());
            }
            Files.createDirectories(dest.getParent());
            Files.move(source, dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new RuntimeException("Failed to move: " + sourcePath, e);
        }
    }

    public void copy(String sourcePath, String destPath) {
        try {
            Path source = resolve(sourcePath);
            Path dest = resolve(destPath);
            if (Files.isDirectory(source)) {
                dest = dest.resolve(source.getFileName());
                Path finalDest = dest;
                try (Stream<Path> walk = Files.walk(source)) {
                    walk.forEach(p -> {
                        try {
                            Files.copy(p, finalDest.resolve(source.relativize(p)), StandardCopyOption.REPLACE_EXISTING);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
            } else {
                Files.createDirectories(dest.getParent());
                Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to copy: " + sourcePath, e);
        }
    }

    public void delete(String relativePath) {
        try {
            Path targetPath = resolve(relativePath);
            if (Files.isDirectory(targetPath)) {
                try (Stream<Path> walk = Files.walk(targetPath)) {
                    walk.sorted(Comparator.reverseOrder())
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

    private Path resolve(String relativePath) {
        String clean = relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;
        return Paths.get(workspaceHolder.getRootPath()).resolve(clean).toAbsolutePath().normalize();
    }
}
