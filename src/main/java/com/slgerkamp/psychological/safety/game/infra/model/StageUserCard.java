package com.slgerkamp.psychological.safety.game.infra.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="stage_user_card")
public class StageUserCard {

    @Id
    public String id;
    public String stageId;
    public String userId;
    public String cardId;
}
