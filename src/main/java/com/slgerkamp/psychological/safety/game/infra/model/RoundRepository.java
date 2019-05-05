package com.slgerkamp.psychological.safety.game.infra.model;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoundRepository extends JpaRepository<Round, Long> {
    
    List<Round> findByStageIdOrderByCreateDateDesc(String stageId);
}
