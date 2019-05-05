package com.slgerkamp.psychological.safety.game.infra.model;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoundCardRepository extends JpaRepository<RoundCard, String> {

    List<RoundCard> findByRoundIdInAndCardIdStartingWith(List<Long> roundIds, String cardId);

    List<RoundCard> findByRoundIdIn(List<Long> roundIds);
}
