package com.slgerkamp.psychological.safety.game.domain.game;

public enum UserStatus {
    NOT_EXIST("not_exsit"),
    APPLY_TO_JOIN("apply_to_join"),
    JOINING("joining");

    public final String status;

    UserStatus(String status) {
        this.status = status;
    }
}
