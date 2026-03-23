package de.itsourcerer.aiideassistant.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectNode {
    
    private String name;
    private String path;
    private NodeType type;
    private List<ProjectNode> children;
    
    public enum NodeType {
        FILE,
        DIRECTORY
    }
    
    public boolean isDirectory() {
        return type == NodeType.DIRECTORY;
    }
    
    public boolean isFile() {
        return type == NodeType.FILE;
    }
}
