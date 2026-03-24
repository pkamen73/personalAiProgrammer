package de.itsourcerer.aiideassistant.service;

import de.itsourcerer.aiideassistant.entity.AnalysisPrompt;
import de.itsourcerer.aiideassistant.repository.AnalysisPromptRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AnalysisPromptService {

    private final AnalysisPromptRepository repository;

    @PostConstruct
    public void initializeDefaultPrompts() {
        if (repository.count() == 0) {
            createDefaultPrompts();
        }
    }

    public void reinitializeDefaults() {
        createDefaultPrompts();
    }

    private void createDefaultPrompts() {
        repository.save(AnalysisPrompt.builder()
            .title("General Analysis")
            .description("Comprehensive analysis of concepts, relationships, and hierarchy")
            .content("Analyze and summarize this mind map. Identify key concepts, relationships, and hierarchy.\n\n" +
                    "IMPORTANT: Items colored RED are PAIN POINTS - these represent problems, challenges, or critical issues. " +
                    "Give special attention to red items in your analysis.\n\n" +
                    "REQUIRED OUTPUT FORMAT:\n" +
                    "1. Provide your analysis as plain text\n" +
                    "2. Then provide a PlantUML diagram in a code block:\n" +
                    "```plantuml\n" +
                    "@startuml\n" +
                    "class ConceptA\n" +
                    "class ConceptB\n" +
                    "ConceptA --> ConceptB\n" +
                    "@enduml\n" +
                    "```")
            .isLocked(true)
            .isDefault(true)
            .build());

        repository.save(AnalysisPrompt.builder()
            .title("Pain Points Focus")
            .description("Deep dive into problems, challenges, and RED-colored critical issues")
            .content("Focus on analyzing PAIN POINTS in this mindmap.\n\n" +
                    "RED-colored items represent problems, challenges, or critical issues.\n\n" +
                    "For each pain point identify:\n" +
                    "1. The specific problem\n" +
                    "2. Its severity/impact\n" +
                    "3. Related concepts\n" +
                    "4. Potential solution areas\n\n" +
                    "OUTPUT FORMAT:\n" +
                    "Provide analysis text, then PlantUML in code block:\n" +
                    "```plantuml\n@startuml\nclass PainPoint #FFAAAA\n@enduml\n```")
            .isLocked(true)
            .isDefault(false)
            .build());

        repository.save(AnalysisPrompt.builder()
            .title("Technical Architecture")
            .description("System design perspective - components, dependencies, and technical structure")
            .content("Analyze this mindmap as a technical system architecture.\n\n" +
                    "Focus on: Components, dependencies, data flow, technical debt (RED items = issues).\n\n" +
                    "REQUIRED OUTPUT: Your analysis, then EXACTLY this format:\n\n" +
                    "```plantuml\n" +
                    "@startuml\n" +
                    "package \"System\" {\n" +
                    "  class ComponentA\n" +
                    "  class ComponentB\n" +
                    "}\n" +
                    "ComponentA --> ComponentB : depends on\n" +
                    "@enduml\n" +
                    "```\n\n" +
                    "Use class diagrams, NOT component syntax. Follow this exact structure.")
            .isLocked(true)
            .isDefault(false)
            .build());

        repository.save(AnalysisPrompt.builder()
            .title("Action Items Extraction")
            .description("Extract actionable tasks, todos, and implementation steps")
            .content("Extract actionable items from this mindmap.\n\n" +
                    "Identify tasks, steps, priorities (RED = high priority), and dependencies.\n\n" +
                    "OUTPUT FORMAT:\n" +
                    "Analysis text, then PlantUML:\n" +
                    "```plantuml\n@startuml\n(*) --> \"Task 1\"\n\"Task 1\" --> \"Task 2\"\n@enduml\n```")
            .isLocked(true)
            .isDefault(false)
            .build());

        repository.save(AnalysisPrompt.builder()
            .title("Simple Structure (Original)")
            .description("Basic concepts and relationships - reliable fallback")
            .content("Analyze and summarize this mind map. Identify key concepts, relationships, and hierarchy. " +
                    "Output a PlantUML class diagram representing the structure.")
            .isLocked(true)
            .isDefault(false)
            .build());

        repository.save(AnalysisPrompt.builder()
            .title("Pain Points Only (Original)")
            .description("Focus on RED items as pain points - reliable fallback")
            .content("Analyze and summarize this mind map. Identify key concepts, relationships, and hierarchy.\n\n" +
                    "IMPORTANT: Items colored RED are PAIN POINTS - these represent problems, challenges, or critical issues. " +
                    "Give special attention to red items in your analysis and clearly mark them as pain points or issues in your response.\n\n" +
                    "Output a PlantUML class diagram representing the structure. Use notes or special styling to highlight pain points.")
            .isLocked(true)
            .isDefault(false)
            .build());

        repository.save(AnalysisPrompt.builder()
            .title("Process & Workflow Analysis")
            .description("Identify processes, workflows, steps, and decision points")
            .content("Analyze this mindmap to identify PROCESSES and WORKFLOWS.\n\n" +
                    "Focus on:\n" +
                    "- Sequential steps and stages\n" +
                    "- Decision points and branches\n" +
                    "- Process flows and cycles\n" +
                    "- Bottlenecks (RED items)\n" +
                    "- Input/output relationships\n\n" +
                    "OUTPUT: Analysis text describing the processes, then PlantUML activity diagram:\n\n" +
                    "```plantuml\n" +
                    "@startuml\n" +
                    "start\n" +
                    ":Process Step 1;\n" +
                    "if (Decision?) then (yes)\n" +
                    "  :Step 2A;\n" +
                    "else (no)\n" +
                    "  :Step 2B;\n" +
                    "endif\n" +
                    ":Final Step;\n" +
                    "stop\n" +
                    "@enduml\n" +
                    "```")
            .isLocked(true)
            .isDefault(false)
            .build());
    }

    public List<AnalysisPrompt> getAllPrompts() {
        return repository.findAll();
    }

    public Optional<AnalysisPrompt> getPromptById(Long id) {
        return repository.findById(id);
    }

    public Optional<AnalysisPrompt> getDefaultPrompt() {
        return repository.findByIsDefaultTrue();
    }

    @Transactional
    public AnalysisPrompt createPrompt(AnalysisPrompt prompt) {
        if (prompt.getIsDefault()) {
            clearAllDefaults();
        }
        return repository.save(prompt);
    }

    @Transactional
    public AnalysisPrompt updatePrompt(Long id, AnalysisPrompt prompt) {
        AnalysisPrompt existing = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Prompt not found: " + id));
        
        if (existing.getIsLocked()) {
            throw new RuntimeException("Cannot modify locked system prompt");
        }
        
        existing.setTitle(prompt.getTitle());
        existing.setDescription(prompt.getDescription());
        existing.setContent(prompt.getContent());
        
        if (prompt.getIsDefault() && !existing.getIsDefault()) {
            clearAllDefaults();
            existing.setIsDefault(true);
        }
        
        return repository.save(existing);
    }

    @Transactional
    public void deletePrompt(Long id) {
        AnalysisPrompt prompt = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Prompt not found: " + id));
        
        if (prompt.getIsLocked()) {
            throw new RuntimeException("Cannot delete locked system prompt");
        }
        
        repository.deleteById(id);
    }

    @Transactional
    public AnalysisPrompt setDefault(Long id) {
        clearAllDefaults();
        
        AnalysisPrompt prompt = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Prompt not found: " + id));
        
        prompt.setIsDefault(true);
        return repository.save(prompt);
    }

    private void clearAllDefaults() {
        List<AnalysisPrompt> allPrompts = repository.findAll();
        allPrompts.forEach(p -> p.setIsDefault(false));
        repository.saveAll(allPrompts);
    }
}
