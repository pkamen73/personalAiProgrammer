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
public class JavaFileInfo {
    private String path;
    private String packageName;
    private String className;
    private List<String> imports;
    private List<String> methods;
    private List<String> fields;
    private boolean isInterface;
    private boolean isAbstract;
    private List<String> annotations;
    private String superClass;
    private List<String> interfaces;
    private String summary;
}
