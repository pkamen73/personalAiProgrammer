package de.itsourcerer.aiideassistant.repository;

import de.itsourcerer.aiideassistant.entity.AnalysisPrompt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnalysisPromptRepository extends JpaRepository<AnalysisPrompt, Long> {
    Optional<AnalysisPrompt> findByIsDefaultTrue();
}
