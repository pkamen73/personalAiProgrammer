package de.itsourcerer.aiideassistant.service;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import de.itsourcerer.aiideassistant.config.WorkspaceHolder;
import de.itsourcerer.aiideassistant.model.JavaFileInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ProjectAnalyzerService {

    private final WorkspaceHolder workspaceHolder;

    private final JavaParser javaParser = new JavaParser();

    public List<JavaFileInfo> analyzeProject() {
        return analyzeProject(null);
    }

    public List<JavaFileInfo> analyzeProject(String overridePath) {
        List<JavaFileInfo> files = new ArrayList<>();
        String root = (overridePath != null && !overridePath.isBlank()) ? overridePath : workspaceHolder.getRootPath();
        Path rootPath = Paths.get(root).toAbsolutePath();
        System.out.println("[ProjectAnalyzer] Scanning: " + rootPath);

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
        String relativePath = rootPath.relativize(filePath).toString();
        ParseResult<CompilationUnit> parseResult = javaParser.parse(filePath);

        if (!parseResult.isSuccessful() || parseResult.getResult().isEmpty()) {
            System.err.println("Parse errors in " + relativePath + ": " + parseResult.getProblems());
            return JavaFileInfo.builder()
                    .path(relativePath)
                    .packageName("")
                    .className(filePath.getFileName().toString().replace(".java", ""))
                    .imports(List.of())
                    .methods(List.of())
                    .fields(List.of())
                    .annotations(List.of())
                    .interfaces(List.of())
                    .build();
        }

        CompilationUnit cu = parseResult.getResult().get();

        String packageName = cu.getPackageDeclaration()
                .map(pd -> pd.getNameAsString())
                .orElse("");

        List<String> imports = cu.getImports().stream()
                .map(id -> id.getNameAsString())
                .collect(Collectors.toList());

        List<String> methods = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        List<String> annotations = new ArrayList<>();
        List<String> interfaces = new ArrayList<>();
        String className = "";
        boolean isInterface = false;
        boolean isAbstract = false;

        List<ClassOrInterfaceDeclaration> types = cu.findAll(ClassOrInterfaceDeclaration.class);
        String superClass = "";
        if (!types.isEmpty()) {
            ClassOrInterfaceDeclaration primary = types.get(0);
            className = primary.getNameAsString();
            isInterface = primary.isInterface();
            isAbstract = primary.isAbstract();

            primary.getAnnotations().forEach(a -> annotations.add(a.getNameAsString()));

            superClass = primary.getExtendedTypes().stream()
                    .findFirst()
                    .map(t -> t.getNameAsString())
                    .orElse("");

            primary.getImplementedTypes()
                    .forEach(t -> interfaces.add(t.getNameAsString()));

            for (MethodDeclaration md : primary.getMethods()) {
                String params = md.getParameters().stream()
                        .map(p -> p.getType().asString() + " " + p.getNameAsString())
                        .collect(Collectors.joining(", "));
                methods.add(md.getType().asString() + " " + md.getNameAsString() + "(" + params + ")");
            }

            for (FieldDeclaration fd : primary.getFields()) {
                fd.getVariables().forEach(v -> fields.add(fd.getElementType().asString() + " " + v.getNameAsString()));
            }
        }

        final String finalClassName = className;
        final String finalSuperClass = superClass;

        return JavaFileInfo.builder()
                .path(relativePath)
                .packageName(packageName)
                .className(finalClassName)
                .imports(imports)
                .methods(methods)
                .fields(fields)
                .annotations(annotations)
                .superClass(finalSuperClass)
                .interfaces(interfaces)
                .isInterface(isInterface)
                .isAbstract(isAbstract)
                .build();
    }
}
