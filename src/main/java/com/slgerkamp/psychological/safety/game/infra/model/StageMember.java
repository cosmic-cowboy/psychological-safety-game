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
@Table(name="stage_member")
public class StageMember {

    @Id
    public String id;
    public String stageId;
    public String userId;
    public String userName;
    public String pictureUrl;
    public String status;
    public Timestamp createDate;
}
