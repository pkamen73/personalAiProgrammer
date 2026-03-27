package de.itsourcerer.aiideassistant.service;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import de.itsourcerer.aiideassistant.config.WorkspaceHolder;
import de.itsourcerer.aiideassistant.entity.ModelConfiguration;
import de.itsourcerer.aiideassistant.model.JavaFileInfo;
import de.itsourcerer.aiideassistant.model.ModelConfig;
import de.itsourcerer.aiideassistant.model.ProjectAnalysisResult;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectAnalysisPipelineService {

    private static final int TOKEN_BUDGET_PER_BATCH = 80_000;
    private static final int MAX_FILES = 500;

    private final ProjectAnalyzerService projectAnalyzerService;
    private final ModelProviderService modelProviderService;
    private final ModelConfigService modelConfigService;
    private final DiagramGenerationService diagramGenerationService;
    private final SimpMessagingTemplate messagingTemplate;

    private final WorkspaceHolder workspaceHolder;

    private final EncodingRegistry encodingRegistry = Encodings.newDefaultEncodingRegistry();
    private final Encoding tokenEncoder = null;

    private Encoding encoder() {
        if (tokenEncoder != null) return tokenEncoder;
        return encodingRegistry.getEncoding(EncodingType.CL100K_BASE);
    }

    public CompletableFuture<ProjectAnalysisResult> runAsync(String ollamaModel, String projectPath) {
        return CompletableFuture.supplyAsync(() -> run(ollamaModel, projectPath));
    }

    public ProjectAnalysisResult run(String ollamaModel, String projectPath) {
        List<String> errors = new ArrayList<>();

        progress("SCANNING", "Scanning and parsing project files…", 0, 0);
        List<JavaFileInfo> files = stage123_scanParseModel(errors, projectPath);
        progress("SCANNING", "Found " + files.size() + " files", files.size(), 0);

        if (files.isEmpty()) {
            progress("ERROR", "No Java files found in project", 0, 0);
            return ProjectAnalysisResult.builder()
                    .status("NO_FILES")
                    .totalFiles(0)
                    .summarizedFiles(0)
                    .errors(errors)
                    .completedAt(Instant.now())
                    .build();
        }

        ModelConfig llmConfig = resolveConfig(ollamaModel, errors);
        if (llmConfig == null) {
            progress("ERROR", "No LLM model configured", files.size(), 0);
            return ProjectAnalysisResult.builder()
                    .status("NO_LLM_CONFIG")
                    .totalFiles(files.size())
                    .summarizedFiles(0)
                    .errors(errors)
                    .completedAt(Instant.now())
                    .build();
        }

        progress("SUMMARIZING", "Generating per-file summaries (0/" + files.size() + ")…", files.size(), 0);
        AtomicInteger summarized = new AtomicInteger(0);
        List<JavaFileInfo> summarizedFiles = stage4_perFileSummaries(files, llmConfig, ollamaModel, summarized, errors);
        progress("SUMMARIZING", "Summaries complete (" + summarized.get() + "/" + files.size() + ")", files.size(), summarized.get());

        progress("SYNTHESIZING", "Synthesizing architecture…", files.size(), summarized.get());
        String architectureSummary = stage5_architectureSynthesis(summarizedFiles, llmConfig, ollamaModel, errors);

        progress("WRITING", "Writing documentation and diagrams…", files.size(), summarized.get());
        String docsPath = stage6_writeDocs(summarizedFiles, architectureSummary, errors);

        progress("COMPLETE", "Analysis complete — docs at " + docsPath, files.size(), summarized.get());

        return ProjectAnalysisResult.builder()
                .status("COMPLETE")
                .totalFiles(files.size())
                .summarizedFiles(summarized.get())
                .architectureSummary(architectureSummary)
                .docsPath(docsPath)
                .errors(errors)
                .completedAt(Instant.now())
                .build();
    }

    private void progress(String stage, String message, int totalFiles, int doneFiles) {
        System.out.println("[Pipeline:" + stage + "] " + message);
        try {
            messagingTemplate.convertAndSend("/topic/progress", java.util.Map.of(
                    "stage", stage,
                    "message", message,
                    "totalFiles", totalFiles,
                    "doneFiles", doneFiles
            ));
        } catch (Exception e) {
            System.err.println("Progress broadcast failed: " + e.getMessage());
        }
    }

    private List<JavaFileInfo> stage123_scanParseModel(List<String> errors, String projectPath) {
        try {
            List<JavaFileInfo> all = projectAnalyzerService.analyzeProject(projectPath);
            if (all.size() > MAX_FILES) {
                System.out.println("Capping at " + MAX_FILES + " files (project has " + all.size() + ")");
                return all.subList(0, MAX_FILES);
            }
            return all;
        } catch (Exception e) {
            errors.add("Scan failed: " + e.getMessage());
            return List.of();
        }
    }

    private List<JavaFileInfo> stage4_perFileSummaries(
            List<JavaFileInfo> files, ModelConfig config, String ollamaModel,
            AtomicInteger summarized, List<String> errors) {

        List<List<JavaFileInfo>> batches = buildTokenBudgetBatches(files);
        System.out.println("[Stage 4] Processing " + batches.size() + " batch(es)...");

        List<JavaFileInfo> result = new ArrayList<>(files);

        for (int bi = 0; bi < batches.size(); bi++) {
            List<JavaFileInfo> batch = batches.get(bi);
            System.out.println("[Stage 4] Batch " + (bi + 1) + "/" + batches.size() + " (" + batch.size() + " files)");

            String prompt = buildPerFileSummaryPrompt(batch);
            try {
                String response = callLlm(prompt, config, ollamaModel);
                parseSummariesIntoFiles(response, batch, result);
                summarized.addAndGet(batch.size());
            } catch (Exception e) {
                errors.add("Batch " + (bi + 1) + " summary failed: " + e.getMessage());
                System.err.println("[Stage 4] Batch error: " + e.getMessage());
            }
        }

        return result;
    }

    private String stage5_architectureSynthesis(
            List<JavaFileInfo> files, ModelConfig config, String ollamaModel, List<String> errors) {
        try {
            String prompt = buildArchitecturePrompt(files);
            return callLlm(prompt, config, ollamaModel);
        } catch (Exception e) {
            errors.add("Architecture synthesis failed: " + e.getMessage());
            return "Architecture synthesis unavailable: " + e.getMessage();
        }
    }

    private String stage6_writeDocs(
            List<JavaFileInfo> files, String architectureSummary, List<String> errors) {
        try {
            Path docsDir = Paths.get(workspaceHolder.getRootPath()).toAbsolutePath().resolve(".ai-ide/docs");
            Files.createDirectories(docsDir);

            Path archPath = docsDir.resolve("ARCHITECTURE.md");
            Files.writeString(archPath, buildArchitectureMarkdown(architectureSummary, files));

            Path indexPath = docsDir.resolve("INDEX.md");
            Files.writeString(indexPath, buildIndexMarkdown(files));

            for (JavaFileInfo file : files) {
                if (file.getSummary() != null && !file.getSummary().isBlank()) {
                    String safeName = file.getPath().replace("/", "_").replace(".java", "") + ".md";
                    Path filePath = docsDir.resolve("files").resolve(safeName);
                    Files.createDirectories(filePath.getParent());
                    Files.writeString(filePath, buildFileMarkdown(file));
                }
            }

            String componentPuml = buildComponentDiagramPuml(files);
            Files.writeString(docsDir.resolve("ARCHITECTURE.puml"), componentPuml);
            try {
                diagramGenerationService.renderToPng(componentPuml, docsDir.resolve("ARCHITECTURE.png"));
                System.out.println("[Stage 6] ARCHITECTURE.png rendered");
            } catch (Exception e) {
                errors.add("ARCHITECTURE.png render failed: " + e.getMessage());
            }

            String classPuml = buildClassHierarchyPuml(files);
            Files.writeString(docsDir.resolve("INDEX.puml"), classPuml);
            try {
                diagramGenerationService.renderToPng(classPuml, docsDir.resolve("INDEX.png"));
                System.out.println("[Stage 6] INDEX.png rendered");
            } catch (Exception e) {
                errors.add("INDEX.png render failed: " + e.getMessage());
            }

            System.out.println("[Stage 6] Docs written to: " + docsDir);
            return docsDir.toString();
        } catch (IOException e) {
            errors.add("Doc writing failed: " + e.getMessage());
            return "";
        }
    }

    private String buildComponentDiagramPuml(List<JavaFileInfo> files) {
        StringBuilder sb = new StringBuilder("@startuml\n");
        sb.append("skinparam shadowing false\n");
        sb.append("skinparam packageStyle rectangle\n");
        sb.append("skinparam component {\n  BackgroundColor #dce8f5\n  BorderColor #4a8fc4\n}\n");
        sb.append("skinparam ArrowColor #333333\n");
        sb.append("title Package Dependency Diagram\n\n");

        java.util.Set<String> allPackages = new java.util.LinkedHashSet<>();
        java.util.Map<String, java.util.Set<String>> packageDeps = new java.util.LinkedHashMap<>();
        String rootPrefix = longestCommonPrefix(files);

        for (JavaFileInfo f : files) {
            String pkg = f.getPackageName().isBlank() ? "(default)" : f.getPackageName();
            allPackages.add(pkg);
            for (String imp : f.getImports()) {
                String impPkg = importToPackage(imp);
                if (!impPkg.equals(pkg) && allPackages.contains(impPkg)) {
                    packageDeps.computeIfAbsent(pkg, k -> new java.util.LinkedHashSet<>()).add(impPkg);
                }
            }
        }

        java.util.Map<String, java.util.Set<String>> groups = new java.util.LinkedHashMap<>();
        for (String pkg : allPackages) {
            String parent = pkg.equals(rootPrefix) ? "(root)" : rootPrefix;
            groups.computeIfAbsent(parent, k -> new java.util.LinkedHashSet<>()).add(pkg);
        }

        if (groups.size() == 1 && groups.containsKey("(root)")) {
            sb.append("package \"").append(rootPrefix).append("\" {\n");
            for (String pkg : allPackages) {
                String label = leafSegment(pkg, rootPrefix);
                sb.append("  [").append(label).append("]\n");
            }
            sb.append("}\n\n");
        } else {
            for (String pkg : allPackages) {
                String label = leafSegment(pkg, rootPrefix);
                sb.append("[").append(label).append("]\n");
            }
            sb.append("\n");
        }

        java.util.Set<String> drawn = new java.util.HashSet<>();
        for (JavaFileInfo f : files) {
            String fromPkg = f.getPackageName().isBlank() ? "(default)" : f.getPackageName();
            String fromLabel = leafSegment(fromPkg, rootPrefix);
            for (String imp : f.getImports()) {
                String toPkg = importToPackage(imp);
                if (allPackages.contains(toPkg) && !toPkg.equals(fromPkg)) {
                    String toLabel = leafSegment(toPkg, rootPrefix);
                    String key = fromLabel + "->" + toLabel;
                    if (!drawn.contains(key)) {
                        sb.append("[").append(fromLabel).append("] --> [").append(toLabel).append("]\n");
                        drawn.add(key);
                    }
                }
            }
        }

        sb.append("@enduml");
        return sb.toString();
    }

    private String buildClassHierarchyPuml(List<JavaFileInfo> files) {
        StringBuilder sb = new StringBuilder("@startuml\n");
        sb.append("skinparam shadowing false\n");
        sb.append("skinparam classAttributeIconSize 0\n");
        sb.append("skinparam class {\n  BackgroundColor #e8f5e9\n  BorderColor #388e3c\n  ArrowColor #1b5e20\n}\n");
        sb.append("title Class Hierarchy Diagram\n\n");

        boolean largeSet = files.size() > 60;
        if (largeSet) sb.append("hide members\n\n");

        java.util.Map<String, java.util.List<JavaFileInfo>> byPackage = new java.util.LinkedHashMap<>();
        for (JavaFileInfo f : files) {
            String pkg = f.getPackageName().isBlank() ? "(default)" : f.getPackageName();
            byPackage.computeIfAbsent(pkg, k -> new java.util.ArrayList<>()).add(f);
        }

        for (java.util.Map.Entry<String, java.util.List<JavaFileInfo>> entry : byPackage.entrySet()) {
            sb.append("package \"").append(entry.getKey()).append("\" {\n");
            for (JavaFileInfo f : entry.getValue()) {
                if (f.getClassName().isBlank()) continue;
                if (f.isInterface()) {
                    sb.append("  interface ").append(f.getClassName());
                } else if (f.isAbstract()) {
                    sb.append("  abstract class ").append(f.getClassName());
                } else {
                    sb.append("  class ").append(f.getClassName());
                }
                if (!largeSet && (!f.getMethods().isEmpty() || !f.getFields().isEmpty())) {
                    sb.append(" {\n");
                    f.getFields().stream().limit(4)
                            .forEach(field -> sb.append("    ").append(escapePuml(field)).append("\n"));
                    f.getMethods().stream().limit(5)
                            .forEach(m -> sb.append("    ").append(escapePuml(m)).append("\n"));
                    sb.append("  }");
                }
                sb.append("\n");
            }
            sb.append("}\n");
        }
        sb.append("\n");

        java.util.Set<String> classNames = files.stream()
                .map(JavaFileInfo::getClassName)
                .filter(n -> !n.isBlank())
                .collect(Collectors.toSet());

        for (JavaFileInfo f : files) {
            if (f.getClassName().isBlank()) continue;
            if (f.getSuperClass() != null && !f.getSuperClass().isBlank()) {
                sb.append(f.getSuperClass()).append(" <|-- ").append(f.getClassName()).append("\n");
            }
            if (f.getInterfaces() != null) {
                for (String iface : f.getInterfaces()) {
                    String base = iface.contains("<") ? iface.substring(0, iface.indexOf('<')) : iface;
                    if (classNames.contains(base)) {
                        sb.append(base).append(" <|.. ").append(f.getClassName()).append("\n");
                    }
                }
            }
        }

        sb.append("@enduml");
        return sb.toString();
    }

    private String escapePuml(String s) {
        return s.replace("<", "~<").replace(">", "~>");
    }

    private String importToPackage(String imp) {
        int lastDot = imp.lastIndexOf('.');
        return lastDot > 0 ? imp.substring(0, lastDot) : imp;
    }

    private String longestCommonPrefix(List<JavaFileInfo> files) {
        return files.stream()
                .map(JavaFileInfo::getPackageName)
                .filter(p -> p != null && !p.isBlank())
                .reduce((a, b) -> {
                    String[] pa = a.split("\\."), pb = b.split("\\.");
                    StringBuilder common = new StringBuilder();
                    for (int i = 0; i < Math.min(pa.length, pb.length); i++) {
                        if (!pa[i].equals(pb[i])) break;
                        if (common.length() > 0) common.append('.');
                        common.append(pa[i]);
                    }
                    return common.toString();
                })
                .orElse("");
    }

    private String leafSegment(String pkg, String prefix) {
        if (pkg.equals(prefix) || prefix.isBlank()) return pkg;
        if (pkg.startsWith(prefix + ".")) return pkg.substring(prefix.length() + 1);
        return pkg;
    }

    private List<List<JavaFileInfo>> buildTokenBudgetBatches(List<JavaFileInfo> files) {
        Encoding enc = encoder();
        List<List<JavaFileInfo>> batches = new ArrayList<>();
        List<JavaFileInfo> current = new ArrayList<>();
        int currentTokens = 0;

        for (JavaFileInfo file : files) {
            String repr = fileRepr(file);
            int tokens = enc.countTokens(repr);
            if (currentTokens + tokens > TOKEN_BUDGET_PER_BATCH && !current.isEmpty()) {
                batches.add(current);
                current = new ArrayList<>();
                currentTokens = 0;
            }
            current.add(file);
            currentTokens += tokens;
        }
        if (!current.isEmpty()) batches.add(current);
        return batches;
    }

    private String callLlm(String prompt, ModelConfig config, String ollamaModel) {
        if (ollamaModel != null && !ollamaModel.isBlank()) {
            var ollamaProvider = (de.itsourcerer.aiideassistant.service.provider.OllamaProvider)
                    modelProviderService.getProvider("ollama");
            StringBuilder sb = new StringBuilder();
            ollamaProvider.streamResponse(prompt, ollamaModel, List.of())
                    .doOnNext(sb::append)
                    .blockLast();
            return sb.toString();
        }
        return modelProviderService.generateResponse(prompt, config);
    }

    private ModelConfig resolveConfig(String ollamaModel, List<String> errors) {
        if (ollamaModel != null && !ollamaModel.isBlank()) {
            return ModelConfig.builder()
                    .provider("ollama")
                    .modelName(ollamaModel)
                    .build();
        }
        return modelConfigService.getDefaultConfig()
                .map(dbCfg -> ModelConfig.builder()
                        .provider(dbCfg.getProvider())
                        .modelName(dbCfg.getModelName())
                        .apiKey(dbCfg.getApiKey())
                        .type(dbCfg.getType() == ModelConfiguration.ModelType.CLOUD
                                ? ModelConfig.ModelType.CLOUD
                                : ModelConfig.ModelType.LOCAL)
                        .build())
                .orElseGet(() -> {
                    errors.add("No default model configured. Pass ollamaModel param or configure a default.");
                    return null;
                });
    }

    private String fileRepr(JavaFileInfo f) {
        return "File: " + f.getPath() + "\n"
                + "Package: " + f.getPackageName() + "\n"
                + "Class: " + f.getClassName() + "\n"
                + "Methods: " + String.join(", ", f.getMethods()) + "\n"
                + "Fields: " + String.join(", ", f.getFields()) + "\n";
    }

    private String buildPerFileSummaryPrompt(List<JavaFileInfo> batch) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a senior software architect. For each Java file below, write a concise 2-3 sentence summary describing its purpose, responsibilities, and key design decisions.\n\n");
        sb.append("Format your response EXACTLY as:\n");
        sb.append("FILE: <path>\nSUMMARY: <summary text>\n\n");
        sb.append("Files to summarize:\n\n");
        for (JavaFileInfo f : batch) {
            sb.append("FILE: ").append(f.getPath()).append("\n");
            sb.append("Package: ").append(f.getPackageName()).append("\n");
            sb.append("Class: ").append(f.getClassName());
            if (!f.getAnnotations().isEmpty()) sb.append(" [@").append(String.join(", @", f.getAnnotations())).append("]");
            if (f.getSuperClass() != null && !f.getSuperClass().isBlank()) sb.append(" extends ").append(f.getSuperClass());
            if (!f.getInterfaces().isEmpty()) sb.append(" implements ").append(String.join(", ", f.getInterfaces()));
            sb.append("\n");
            if (!f.getMethods().isEmpty()) sb.append("Methods: ").append(String.join(" | ", f.getMethods())).append("\n");
            if (!f.getFields().isEmpty()) sb.append("Fields: ").append(String.join(", ", f.getFields())).append("\n");
            sb.append("\n");
        }
        return sb.toString();
    }

    private String buildArchitecturePrompt(List<JavaFileInfo> files) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a senior software architect. Based on the per-file summaries below, write a comprehensive architecture overview of this Java project.\n\n");
        sb.append("Include:\n- High-level purpose and domain\n- Key architectural patterns (MVC, layered, event-driven, etc.)\n- Main components and their relationships\n- Data flow\n- Notable design decisions\n\n");
        sb.append("File summaries:\n\n");
        for (JavaFileInfo f : files) {
            if (f.getSummary() != null && !f.getSummary().isBlank()) {
                sb.append("### ").append(f.getClassName()).append(" (").append(f.getPath()).append(")\n");
                sb.append(f.getSummary()).append("\n\n");
            } else {
                sb.append("### ").append(f.getClassName()).append("\n");
                sb.append("Package: ").append(f.getPackageName()).append(" | Methods: ").append(f.getMethods().size()).append("\n\n");
            }
        }
        return sb.toString();
    }

    private void parseSummariesIntoFiles(String response, List<JavaFileInfo> batch, List<JavaFileInfo> allFiles) {
        String[] lines = response.split("\n");
        String currentPath = null;
        StringBuilder currentSummary = new StringBuilder();

        for (String line : lines) {
            if (line.startsWith("FILE:")) {
                if (currentPath != null) {
                    setSummary(currentPath, currentSummary.toString().trim(), batch, allFiles);
                }
                currentPath = line.substring(5).trim();
                currentSummary = new StringBuilder();
            } else if (line.startsWith("SUMMARY:") && currentPath != null) {
                currentSummary.append(line.substring(8).trim());
            } else if (currentPath != null && !line.isBlank()) {
                currentSummary.append(" ").append(line.trim());
            }
        }
        if (currentPath != null) {
            setSummary(currentPath, currentSummary.toString().trim(), batch, allFiles);
        }
    }

    private void setSummary(String path, String summary, List<JavaFileInfo> batch, List<JavaFileInfo> allFiles) {
        for (JavaFileInfo f : allFiles) {
            if (f.getPath().equals(path) || f.getPath().endsWith(path)) {
                f.setSummary(summary);
                return;
            }
        }
    }

    private String buildArchitectureMarkdown(String architectureSummary, List<JavaFileInfo> files) {
        StringBuilder md = new StringBuilder();
        md.append("# Project Architecture\n\n");
        md.append("*Generated by AI Project Analysis Pipeline — ").append(Instant.now()).append("*\n\n");
        md.append("---\n\n");
        md.append(architectureSummary).append("\n\n");
        md.append("---\n\n");
        md.append("## File Inventory\n\n");
        md.append("| File | Class | Package |\n|------|-------|--------|\n");
        for (JavaFileInfo f : files) {
            md.append("| `").append(f.getPath()).append("` | ")
              .append(f.getClassName()).append(" | ")
              .append(f.getPackageName()).append(" |\n");
        }
        return md.toString();
    }

    private String buildIndexMarkdown(List<JavaFileInfo> files) {
        StringBuilder md = new StringBuilder();
        md.append("# File Index\n\n");
        md.append("*Generated by AI Project Analysis Pipeline — ").append(Instant.now()).append("*\n\n");

        String packages = files.stream()
                .map(JavaFileInfo::getPackageName)
                .filter(p -> p != null && !p.isBlank())
                .distinct().sorted()
                .collect(Collectors.joining("\n- ", "- ", ""));

        md.append("## Packages\n\n").append(packages).append("\n\n");
        md.append("## Files\n\n");

        for (JavaFileInfo f : files) {
            String safeName = f.getPath().replace("/", "_").replace(".java", "") + ".md";
            md.append("- [`").append(f.getClassName()).append("`](files/").append(safeName).append(") — ").append(f.getPath()).append("\n");
        }

        return md.toString();
    }

    private String buildFileMarkdown(JavaFileInfo f) {
        StringBuilder md = new StringBuilder();
        md.append("# ").append(f.getClassName()).append("\n\n");
        md.append("**Path:** `").append(f.getPath()).append("`  \n");
        md.append("**Package:** `").append(f.getPackageName()).append("`\n\n");

        if (!f.getAnnotations().isEmpty()) {
            md.append("**Annotations:** ").append(f.getAnnotations().stream().map(a -> "@" + a).collect(Collectors.joining(", "))).append("\n\n");
        }
        if (f.getSuperClass() != null && !f.getSuperClass().isBlank()) {
            md.append("**Extends:** `").append(f.getSuperClass()).append("`\n\n");
        }
        if (!f.getInterfaces().isEmpty()) {
            md.append("**Implements:** ").append(f.getInterfaces().stream().map(i -> "`" + i + "`").collect(Collectors.joining(", "))).append("\n\n");
        }

        if (f.getSummary() != null && !f.getSummary().isBlank()) {
            md.append("## Summary\n\n").append(f.getSummary()).append("\n\n");
        }

        if (!f.getMethods().isEmpty()) {
            md.append("## Methods\n\n");
            f.getMethods().forEach(m -> md.append("- `").append(m).append("`\n"));
            md.append("\n");
        }

        if (!f.getFields().isEmpty()) {
            md.append("## Fields\n\n");
            f.getFields().forEach(field -> md.append("- `").append(field).append("`\n"));
            md.append("\n");
        }

        return md.toString();
    }
}
