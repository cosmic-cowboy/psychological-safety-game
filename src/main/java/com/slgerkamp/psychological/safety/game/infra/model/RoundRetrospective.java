package com.slgerkamp.psychological.safety.game.infra.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="round_retrospective")
public class RoundRetrospective {

    @Id
    public String id;
    public Long roundId;
    public String userId;
    public String cardId;
    public String answer;
    public Timestamp createDate;
}
