package com.slgerkamp.psychological.safety.game.infra.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface StageUserCardRepository extends JpaRepository<StageUserCard, String> {

    List<StageUserCard> findByStageId(String stageId);

    @Transactional
    void deleteByStageId(String stageId);

    @Transactional
    void deleteByStageIdAndUserIdAndCardId(String stageId, String userId, String cardId);

}
