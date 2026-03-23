package de.itsourcerer.aiideassistant.repository;

import de.itsourcerer.aiideassistant.entity.ModelConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ModelConfigRepository extends JpaRepository<ModelConfiguration, Long> {
    Optional<ModelConfiguration> findByIsDefaultTrue();
}
