package de.itsourcerer.aiideassistant.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelConfig {
    
    private String id;
    private String provider;
    private String modelName;
    private String apiKey;
    private String endpoint;
    private ModelType type;
    
    public enum ModelType {
        CLOUD,
        LOCAL
    }
    
    public boolean isLocal() {
        return type == ModelType.LOCAL;
    }
    
    public boolean isCloud() {
        return type == ModelType.CLOUD;
    }
}
