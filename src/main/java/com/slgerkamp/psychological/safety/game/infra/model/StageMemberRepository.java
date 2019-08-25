package com.slgerkamp.psychological.safety.game.infra.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface StageMemberRepository extends JpaRepository<StageMember, String> {

    List<StageMember> findByUserIdAndStatus(String userId, String status);

    List<StageMember> findByUserIdAndStatusIn(String userId, List<String> statusList);

    List<StageMember> findByStageIdAndStatusIn(String stageId, List<String> statusList);

    List<StageMember> findByStageId(String stageId);

    @Transactional
    void deleteByUserIdAndStatusIn(String userId, List<String> statusList);

    @Transactional
    void deleteByStageId(String stageId);

}
