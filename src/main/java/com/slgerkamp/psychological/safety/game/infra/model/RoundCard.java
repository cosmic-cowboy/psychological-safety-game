package com.slgerkamp.psychological.safety.game.infra.model;

import javax.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="round_card")
public class RoundCard {

    @Id
    public String id;
    public Long roundId;
    public String userId;
    public String cardId;
    public String word;
    public Timestamp createDate;
}
