package com.slgerkamp.psychological.safety.game.domain.game;

public enum CardType {
    SITUATION("card.type.situation.label"),
    COMMENT("card.type.comment.label"),
    THEME("card.type.theme.label");

    public final String message;

    CardType(String message) {this.message = message; }
}
