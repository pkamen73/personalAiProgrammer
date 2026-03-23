package de.itsourcerer.aiideassistant.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "model_configurations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelConfiguration {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String provider;
    
    @Column(nullable = false)
    private String modelName;
    
    @Column(nullable = false)
    private String displayName;
    
    @Column(length = 1000)
    private String apiKey;
    
    @Column(nullable = false)
    private Boolean isDefault = false;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ModelType type;
    
    public enum ModelType {
        CLOUD,
        LOCAL
    }
}
