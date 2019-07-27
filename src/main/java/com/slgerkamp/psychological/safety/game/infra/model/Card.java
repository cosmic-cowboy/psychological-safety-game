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
@Table(name="card")
public class Card {

    @Id
    public String id;
    public String type;
    public String title;
    public String text;
    public Timestamp createDate;
    public Timestamp updateDate;
}
