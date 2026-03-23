package de.itsourcerer.aiideassistant.service;

import de.itsourcerer.aiideassistant.model.JavaFileInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
public class ProjectAnalyzerService {

    @Value("${workspace.root-path:.}")
    private String workspaceRoot;

    private static final Pattern PACKAGE_PATTERN = Pattern.compile("package\\s+([\\w.]+);");
    private static final Pattern CLASS_PATTERN = Pattern.compile("(?:public\\s+)?(?:abstract\\s+)?(?:final\\s+)?(class|interface|enum)\\s+(\\w+)");
    private static final Pattern IMPORT_PATTERN = Pattern.compile("import\\s+([\\w.]+);");
    private static final Pattern METHOD_PATTERN = Pattern.compile("(?:public|protected|private)?\\s+(?:static\\s+)?(?:\\w+(?:<[^>]+>)?\\s+)(\\w+)\\s*\\([^)]*\\)");

    public List<JavaFileInfo> analyzeProject() {
        List<JavaFileInfo> files = new ArrayList<>();
        Path rootPath = Paths.get(workspaceRoot).toAbsolutePath();

        try (Stream<Path> paths = Files.walk(rootPath)) {
            paths.filter(p -> p.toString().endsWith(".java"))
                 .filter(p -> !p.toString().contains("/target/"))
                 .filter(p -> !p.toString().contains("/."))
                 .forEach(path -> {
                     try {
                         JavaFileInfo info = analyzeJavaFile(path, rootPath);
                         if (info != null) {
                             files.add(info);
                         }
                     } catch (Exception e) {
                         System.err.println("Failed to analyze: " + path + " - " + e.getMessage());
                     }
                 });
        } catch (IOException e) {
            throw new RuntimeException("Failed to analyze project", e);
        }

        return files;
    }

    private JavaFileInfo analyzeJavaFile(Path filePath, Path rootPath) throws IOException {
        String content = Files.readString(filePath);
        String relativePath = rootPath.relativize(filePath).toString();

        String packageName = extractPackage(content);
        String className = extractClassName(content);
        List<String> imports = extractImports(content);
        List<String> methods = extractMethods(content);
        List<String> fields = extractFields(content);

        return JavaFileInfo.builder()
                .path(relativePath)
                .packageName(packageName)
                .className(className)
                .imports(imports)
                .methods(methods)
                .fields(fields)
                .isInterface(content.contains("interface " + className))
                .isAbstract(content.contains("abstract class " + className))
                .build();
    }

    private String extractPackage(String content) {
        Matcher matcher = PACKAGE_PATTERN.matcher(content);
        return matcher.find() ? matcher.group(1) : "";
    }

    private String extractClassName(String content) {
        Matcher matcher = CLASS_PATTERN.matcher(content);
        return matcher.find() ? matcher.group(2) : "";
    }

    private List<String> extractImports(String content) {
        List<String> imports = new ArrayList<>();
        Matcher matcher = IMPORT_PATTERN.matcher(content);
        while (matcher.find()) {
            imports.add(matcher.group(1));
        }
        return imports;
    }

    private List<String> extractMethods(String content) {
        List<String> methods = new ArrayList<>();
        Matcher matcher = METHOD_PATTERN.matcher(content);
        while (matcher.find()) {
            methods.add(matcher.group(1));
        }
        return methods;
    }

    private List<String> extractFields(String content) {
        List<String> fields = new ArrayList<>();
        Pattern fieldPattern = Pattern.compile("private\\s+(?:static\\s+)?(?:final\\s+)?\\w+(?:<[^>]+>)?\\s+(\\w+);");
        Matcher matcher = fieldPattern.matcher(content);
        while (matcher.find()) {
            fields.add(matcher.group(1));
        }
        return fields;
    }
}
