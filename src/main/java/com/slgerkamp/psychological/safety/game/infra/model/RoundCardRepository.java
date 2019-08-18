package com.slgerkamp.psychological.safety.game.infra.model;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoundCardRepository extends JpaRepository<RoundCard, String> {

    List<RoundCard> findByRoundIdInAndCardIdStartingWith(List<Long> roundIds, String cardId);

    List<RoundCard> findByRoundIdInOrderByCreateDateDesc(List<Long> roundIds);

    Optional<RoundCard> findFirstByRoundIdInOrderByCreateDateDesc(List<Long> roundIds);
}
