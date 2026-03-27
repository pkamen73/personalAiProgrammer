package de.itsourcerer.aiideassistant.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectAnalysisResult {
    private String status;
    private int totalFiles;
    private int summarizedFiles;
    private String architectureSummary;
    private String docsPath;
    private List<String> errors;
    private Instant completedAt;
}
