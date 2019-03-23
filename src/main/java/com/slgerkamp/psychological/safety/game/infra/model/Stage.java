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
@Table(name="stage")
public class Stage {

    @Id
    public String id;
    public String password;
    public String name;
    public String status;
    public Timestamp createDate;
    public Timestamp updateDate;
}
