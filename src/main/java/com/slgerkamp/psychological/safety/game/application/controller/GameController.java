package com.slgerkamp.psychological.safety.game.application.controller;

import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.PostbackEvent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.spring.boot.annotation.EventMapping;
import com.linecorp.bot.spring.boot.annotation.LineMessageHandler;
import com.slgerkamp.psychological.safety.game.domain.game.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

@LineMessageHandler
public class GameController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GameController.class);

    @Autowired
    private StageService stageService;

    @Autowired
    private RoundService roundService;

    @Autowired
    private UserService userService;


    @EventMapping
    public void handleTextMessageEvent(MessageEvent<TextMessageContent> event) {

        final String userId = event.getSource().getUserId();
        final String message = event.getMessage().getText();
        final UserStatus userStatus = userService.userStatus(userId);

        switch (userStatus) {
            case APPLY_TO_JOIN:
                stageService.confirmPasswordToJoinAStage(userId, message);
                break;

            case JOINING:
                roundService.setFeelings(userId, message);
                break;

            default:
                break;
        }
    }

    @EventMapping
    public void handlePostbackEvent(PostbackEvent event) {

        final String userId = event.getSource().getUserId();
        final Map<String, String> map = parseMap(event.getPostbackContent().getData());

        if (map.containsKey(PostBackKeyName.ACTION.keyName)) {
            final PostBackAction postBackAction = PostBackAction.valueOf(map.get(PostBackKeyName.ACTION.keyName));
            log.debug("postback action : " + postBackAction.actionName);
            switch (postBackAction) {
                case CREATE:
                    stageService.createStage(userId);
                    break;

                case GET_STAGES:
                    stageService.getStagesParticipantsWanted(userId);
                    break;

                case REQUEST_TO_JOIN_STAGE:
                    String stageId = map.get(PostBackKeyName.STAGE.keyName);
                    stageService.selectStageToJoin(userId,stageId);
                    break;

                case REQUEST_TO_START_ROUND:
                    roundService.requestToStartRound(userId);
                    break;

                case CONFIRM_TO_START_ROUND:
                    roundService.confirmToStartRound(userId);
                    break;

                case SET_ROUND_CARD:
                    roundService.setRoundCard(userId);
                    break;

                default:
                    break;
            }
        }
    }

    private Map<String, String> parseMap(final String input) {
        log.debug("postback event : " + input);
        final Map<String, String> map = new HashMap<String, String>();
        for (String pair : input.split("&")) {
            String[] kv = pair.split("=");
            map.put(kv[0], kv[1]);
        }
        return map;
    }
}
