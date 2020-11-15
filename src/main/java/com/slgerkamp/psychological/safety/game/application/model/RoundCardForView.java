package com.slgerkamp.psychological.safety.game.application.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoundCardForView {

    public Long roundId;
    public String userId;
    public String cardId;
    public String type;
    public String text;
    public String createDate;

}
