package de.itsourcerer.aiideassistant.service;

import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DiagramGenerationService {

    @Value("${workspace.root-path:.}")
    private String workspaceRoot;

    private static final String DIAGRAMS_DIR = ".ai-ide/diagrams";
    private static final Pattern PLANTUML_PATTERN = Pattern.compile("```(?:plantuml)?\\s*\\n?(@startuml[\\s\\S]*?@enduml)\\s*\\n?```", Pattern.CASE_INSENSITIVE);
    private static final Pattern PLANTUML_NAKED = Pattern.compile("(@startuml[\\s\\S]*?@enduml)", Pattern.CASE_INSENSITIVE);

    @PostConstruct
    public void init() {
        System.setProperty("PLANTUML_LIMIT_SIZE", "8192");
        System.setProperty("plantuml.include.path", ".");
        System.setProperty("GRAPHVIZ_DOT", "");
        System.out.println("PlantUML configured with Smetana (built-in Java renderer)");
    }

    public String generateDiagram(String aiResponse, String originalImageId) throws Exception {
        System.out.println("Attempting to extract PlantUML from response...");
        String plantUmlCode = extractPlantUML(aiResponse);
        
        if (plantUmlCode == null || plantUmlCode.isEmpty()) {
            System.out.println("No PlantUML found, using default template");
            plantUmlCode = generateDefaultPlantUML(aiResponse);
        } else {
            System.out.println("✓ Extracted PlantUML, length: " + plantUmlCode.length());
        }
        
        if (!plantUmlCode.contains("!pragma layout")) {
            plantUmlCode = plantUmlCode.replace("@startuml", "@startuml\n!pragma layout smetana");
        }
        
        System.out.println("Generating PNG from PlantUML...");
        System.out.println("PlantUML code preview: " + plantUmlCode.substring(0, Math.min(200, plantUmlCode.length())));
        
        ByteArrayOutputStream pngStream = new ByteArrayOutputStream();
        SourceStringReader reader = new SourceStringReader(plantUmlCode);
        
        FileFormatOption option = new FileFormatOption(FileFormat.PNG);
        reader.outputImage(pngStream, option);
        
        System.out.println("✓ PlantUML rendered, PNG size: " + pngStream.size() + " bytes");
        
        String diagramId = "diagram-" + originalImageId.replace(".", "-") + ".png";
        Path diagramDir = Paths.get(workspaceRoot).toAbsolutePath().resolve(DIAGRAMS_DIR);
        System.out.println("Creating diagram directory: " + diagramDir);
        Files.createDirectories(diagramDir);
        
        Path diagramPath = diagramDir.resolve(diagramId);
        System.out.println("Writing diagram to: " + diagramPath);
        Files.write(diagramPath, pngStream.toByteArray());
        System.out.println("✓ Diagram saved: " + diagramId);
        
        String analysisFileName = diagramId.replace(".png", ".txt");
        Path analysisPath = diagramDir.resolve(analysisFileName);
        System.out.println("Writing analysis text to: " + analysisPath);
        Files.writeString(analysisPath, aiResponse);
        System.out.println("✓ Analysis text saved: " + analysisFileName);
        
        return diagramId;
    }

    private String extractPlantUML(String text) {
        System.out.println("=== Extracting PlantUML ===");
        System.out.println("Text length: " + text.length());
        System.out.println("Text preview: " + text.substring(0, Math.min(300, text.length())));
        
        Matcher matcher = PLANTUML_PATTERN.matcher(text);
        if (matcher.find()) {
            System.out.println("✓ Matched with PLANTUML_PATTERN (code block)");
            return matcher.group(1);
        }
        
        System.out.println("Code block pattern failed, trying naked pattern...");
        matcher = PLANTUML_NAKED.matcher(text);
        if (matcher.find()) {
            System.out.println("✓ Matched with PLANTUML_NAKED");
            return matcher.group(1);
        }
        
        System.out.println("✗ No PlantUML patterns matched");
        return null;
    }

    private String generateDefaultPlantUML(String aiResponse) {
        return "@startuml\n" +
               "title Mind Map Analysis\n" +
               "note as N1\n" +
               aiResponse.replace("\n", "\\n").substring(0, Math.min(500, aiResponse.length())) +
               "\nend note\n" +
               "@enduml";
    }

    public byte[] getDiagram(String diagramId) throws Exception {
        Path diagramPath = Paths.get(workspaceRoot, DIAGRAMS_DIR, diagramId);
        if (!Files.exists(diagramPath)) {
            throw new Exception("Diagram not found: " + diagramId);
        }
        return Files.readAllBytes(diagramPath);
    }

    public byte[] generatePreview(String analysisText) throws Exception {
        String plantUmlCode = extractPlantUML(analysisText);
        
        if (plantUmlCode == null || plantUmlCode.isEmpty()) {
            plantUmlCode = generateDefaultPlantUML(analysisText);
        }
        
        if (!plantUmlCode.contains("!pragma layout")) {
            plantUmlCode = plantUmlCode.replace("@startuml", "@startuml\n!pragma layout smetana");
        }
        
        ByteArrayOutputStream pngStream = new ByteArrayOutputStream();
        SourceStringReader reader = new SourceStringReader(plantUmlCode);
        FileFormatOption option = new FileFormatOption(FileFormat.PNG);
        reader.outputImage(pngStream, option);
        
        return pngStream.toByteArray();
    }
}
