package com.slgerkamp.psychological.safety.game.infra.model;

import javax.persistence.Entity;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="stage-member")
public class StageMember {

    public Long stageId;
    public Integer stageNumber;
    public String userId;
    public String userName;
}
