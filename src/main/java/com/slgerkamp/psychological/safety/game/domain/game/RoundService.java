package com.slgerkamp.psychological.safety.game.domain.game;

import org.springframework.stereotype.Component;

@Component
public class RoundService {

    public void requestToStartRound(String userId) {
        // check which stage member sender is
        // send confirmation message to stage members for asking to start round
    }

    public void confirmToStartRound(String userId) {
        // check which stage member sender is
        // change stage status

        // set round cards for stage members
        // send round cards to stage members
    }

    public void setRoundCard(String userId) {
        // get which stage member sender is
        // check correct turn person or not
        // set Card on this turn
        // get Card on this turn
        // send message about "what card turn person set" to stage members
        // send message about "who is next turn" to stage members
    }


    public void setFeelings(String userId, String message) {
        // get which stage member sender is
        // check correct turn person or not
        // set message
    }
}
