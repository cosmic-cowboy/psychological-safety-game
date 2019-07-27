package com.slgerkamp.psychological.safety.game.infra.model;

import org.springframework.data.jpa.repository.JpaRepository;


public interface CardRepository extends JpaRepository<Card, String> {
}
