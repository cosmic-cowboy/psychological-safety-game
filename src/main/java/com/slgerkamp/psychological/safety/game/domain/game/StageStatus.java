package com.slgerkamp.psychological.safety.game.domain.game;

public enum StageStatus {
    PARTICIPANTS_WANTED("participants_wanted"),
    START_GAME("start_game"),
    END_GAME("end_game");

    public final String status;

    StageStatus(String status) {
        this.status = status;
    }
}
