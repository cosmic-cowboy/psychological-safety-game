package com.slgerkamp.psychological.safety.game.infra.model;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StageRepository extends JpaRepository<Stage, String> {

    @Query("SELECT u FROM Stage u WHERE u.status = 'PARTICIPANTS_WANTED' ORDER BY u.createDate DESC")
    List<Stage> findParticipantsWantedStageList(Pageable pageable);

}
