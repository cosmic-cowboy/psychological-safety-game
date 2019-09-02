package com.slgerkamp.psychological.safety.game.infra.model;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoundRetrospectiveRepository extends JpaRepository<RoundRetrospective, String> {

    List<RoundRetrospective> findByRoundIdInOrderByCreateDateAsc(List<Long> roundIds);

    List<RoundRetrospective> findByRoundIdOrderByCreateDateDesc(Long roundId);

}
