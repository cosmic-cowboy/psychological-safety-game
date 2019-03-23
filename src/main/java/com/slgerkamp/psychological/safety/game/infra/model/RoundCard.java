package com.slgerkamp.psychological.safety.game.infra.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="round_card")
public class RoundCard {

    @Id
    public Integer roundId;
    public Integer turnNumber;
    public String cardId;
    public String word;
}
