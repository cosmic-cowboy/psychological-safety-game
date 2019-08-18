package com.slgerkamp.psychological.safety.game.domain.game;

public enum PostBackKeyName {
    ACTION("action"),
    STAGE("stage"),
    ROUND("round"),
    CARD("card"),
    THEME_ANSWER("theme_answer");

    public final String keyName;

    PostBackKeyName(String keyName) {
        this.keyName = keyName;
    }

    }
