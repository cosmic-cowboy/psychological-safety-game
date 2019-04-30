package com.slgerkamp.psychological.safety.game.infra.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface StageMemberRepository extends JpaRepository<StageMember, String> {

    List<StageMember> findByUserIdAndStatus(String userId, String status);

    List<StageMember> findByUserIdAndStatusAndStageId(String userId, String status, String stageId);

    List<StageMember> findByUserId(String userId);

    List<StageMember> findByStageId(String stageId);

    @Transactional
    void deleteByUserId(String userId);

    @Transactional
    void deleteByStageId(String stageId);

}
