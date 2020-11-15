package com.slgerkamp.psychological.safety.game.infra.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="round")
public class Round {

    @Id
    public Long id;
    public String stageId;
    public Integer currentRoundNumber;
    public String situationCardId;
    public String status;
    public Timestamp createDate;
}
