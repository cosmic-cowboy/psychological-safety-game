package com.slgerkamp.psychological.safety.game.domain.game;

public enum PostBackAction {
    CREATE("ステージ作成"),
    GET_STAGES("参加可能なステージ一覧"),
    REQUEST_TO_JOIN_STAGE("ステージ参加リクエスト"),
    REQUEST_TO_START_ROUND("ラウンドスタートリクエスト"),
    CONFIRM_TO_START_ROUND("ラウンドスタート確認"),
    SET_ROUND_CARD("ラウンドカード受け取り");

    public final String actionName;

    PostBackAction(String actionName) {
        this.actionName = actionName;
    }
}
