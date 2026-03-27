package de.itsourcerer.aiideassistant.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class WorkspaceHolder {

    private volatile String rootPath;

    public WorkspaceHolder(@Value("${workspace.root-path:.}") String rootPath) {
        this.rootPath = rootPath;
    }

    public String getRootPath() {
        return rootPath;
    }

    public String getAbsolutePath() {
        return Paths.get(rootPath).toAbsolutePath().normalize().toString();
    }

    public void setRootPath(String path) {
        this.rootPath = Paths.get(path).toAbsolutePath().normalize().toString();
    }

    public Path resolve(String relativePath) {
        String clean = relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;
        return Paths.get(rootPath).toAbsolutePath().normalize().resolve(clean);
    }
}
