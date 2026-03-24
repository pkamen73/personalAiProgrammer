package de.itsourcerer.aiideassistant.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "analysis_prompts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisPrompt {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(length = 500)
    private String description;
    
    @Column(length = 5000, nullable = false)
    private String content;
    
    @Column(nullable = false)
    private Boolean isLocked = false;
    
    @Column(nullable = false)
    private Boolean isDefault = false;
    
    private Instant createdAt;
    private Instant updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
