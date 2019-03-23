package com.slgerkamp.psychological.safety.game.domain.game;

public enum PostBackKeyName {
    ACTION("action"),
    STAGE("stage");

    public final String keyName;

    PostBackKeyName(String keyName) {
        this.keyName = keyName;
    }

    }
