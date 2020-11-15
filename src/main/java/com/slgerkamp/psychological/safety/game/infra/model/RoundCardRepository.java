package com.slgerkamp.psychological.safety.game.infra.model;

import com.slgerkamp.psychological.safety.game.domain.game.CardType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RoundCardRepository extends JpaRepository<RoundCard, String> {

    List<RoundCard> findByRoundIdInOrderByCreateDateAsc(List<Long> roundIds);

}
