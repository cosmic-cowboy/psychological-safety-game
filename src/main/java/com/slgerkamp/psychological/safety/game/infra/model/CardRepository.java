package com.slgerkamp.psychological.safety.game.infra.model;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CardRepository extends JpaRepository<Card, String> {

    List<Card> findByTypeOrderByCreateDate(String type);

    List<Card> findByTypeAndIdStartsWithOrderByCreateDate(String type, String id);

    List<Card> findByIdIn(List<String> list);

}
