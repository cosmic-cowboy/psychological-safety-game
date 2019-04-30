package com.slgerkamp.psychological.safety.game.domain.game;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum Cards {
    SITUATION01("situation"),
    SITUATION02("situation"),
    SITUATION03("situation"),
    SITUATION04("situation"),
    SITUATION05("situation"),
    SITUATION06("situation"),
    SITUATION07("situation"),
    SITUATION08("situation"),
    SITUATION09("situation"),
    SITUATION10("situation"),
    SITUATION11("situation"),
    SITUATION12("situation"),
    SITUATION13("situation"),
    SITUATION14("situation"),
    COMMENT01("comment"),
    COMMENT02("comment"),
    COMMENT03("comment"),
    COMMENT04("comment"),
    COMMENT05("comment"),
    COMMENT06("comment"),
    COMMENT07("comment"),
    COMMENT08("comment"),
    COMMENT09("comment"),
    COMMENT10("comment"),
    COMMENT11("comment"),
    COMMENT12("comment"),
    COMMENT13("comment"),
    COMMENT14("comment"),
    COMMENT15("comment"),
    COMMENT16("comment"),
    COMMENT17("comment"),
    COMMENT18("comment"),
    COMMENT19("comment"),
    COMMENT20("comment"),
    COMMENT21("comment"),
    COMMENT22("comment"),
    COMMENT23("comment"),
    COMMENT24("comment"),
    COMMENT25("comment"),
    COMMENT26("comment"),
    COMMENT27("comment"),
    COMMENT28("comment"),
    COMMENT29("comment"),
    OPTION01("option"),
    OPTION02("option"),
    OPTION03("option"),
    OPTION04("option"),
    OPTION05("option"),
    OPTION06("option"),
    OPTION07("option"),
    OPTION08("option"),
    OPTION09("option"),
    THEME01("theme"),
    THEME02("theme"),
    THEME03("theme"),
    THEME04("theme"),
    THEME05("theme"),
    THEME06("theme");

    public final String type;

    Cards(String type) {
        this.type = type;
    }

    public static List<String> getTypeList(final String type) {
        return Arrays.stream(values())
                .filter(card -> card.type.equals(type))
                .map(card -> card.name())
                .collect(Collectors.toList());
    }

    public static String getFileName(final String filename) {
        return "https://psychological-safety-game.herokuapp.com/cards/" + filename + ".jpg";
    }
}
