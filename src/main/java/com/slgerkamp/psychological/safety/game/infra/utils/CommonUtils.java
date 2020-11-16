package com.slgerkamp.psychological.safety.game.infra.utils;

import java.util.Random;
import java.util.UUID;

public final class CommonUtils {

    private static Random rnd = new Random();

    private CommonUtils() { };

    public static String getUUID() {
        return UUID.randomUUID().toString().replace("-","");
    }

    public static String get6DigitCode() {
        return String.format("%06d", rnd.nextInt(999999));
    }

    public static String createStageDashboardUrl(String stageId) {
        final String url = "https://psychological-safety-game.herokuapp.com/stage/" + stageId;
        return url;
    }

    public static String createStageJoinUrl(String stageId) {
        final String url = "https://psychological-safety-game.herokuapp.com/stage/" + stageId + "/join";
        return url;
    }

}
