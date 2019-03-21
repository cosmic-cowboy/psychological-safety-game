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
@Table(name="round")
public class Round {

    @Id
    public Integer id;
    public Long stageId;
    public Integer currentTurnNumber;
    public String theme;
}
