package com.slgerkamp.psychological.safety.game.application.controller;

import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.PostbackEvent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.spring.boot.annotation.EventMapping;
import com.linecorp.bot.spring.boot.annotation.LineMessageHandler;
import com.slgerkamp.psychological.safety.game.domain.game.*;
import com.slgerkamp.psychological.safety.game.domain.game.service.RoundService;
import com.slgerkamp.psychological.safety.game.domain.game.service.StageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

@LineMessageHandler
public class GameController {

    private static final Logger log = LoggerFactory.getLogger(GameController.class);

    @Autowired
    private StageService stageService;
    @Autowired
    private RoundService roundService;

    @EventMapping
    public void handleTextMessageEvent(MessageEvent<TextMessageContent> event) {
        log.debug("event action : " + event.getMessage());
    }

    @EventMapping
    public void handlePostbackEvent(PostbackEvent event) {

        final String userId = event.getSource().getUserId();
        final Map<String, String> map = parseMap(event.getPostbackContent().getData());

        if (map.containsKey(PostBackKeyName.ACTION.keyName)) {
            final PostBackAction postBackAction = PostBackAction.valueOf(map.get(PostBackKeyName.ACTION.keyName));
            log.debug("postback action : " + postBackAction.name());

            switch (postBackAction) {
                case CREATE:
                    stageService.createStageTable(userId);
                    break;

                case REQUEST_TO_START_STAGE:
                    stageService.requestToStartStage(userId);
                    break;

                case CONFIRM_TO_START_STAGE:
                    String stageId_CONFIRM_TO_START_ROUND = map.get(PostBackKeyName.STAGE.keyName);
                    stageService.confirmToStartStage(userId, stageId_CONFIRM_TO_START_ROUND);
                    break;

                case SET_ROUND_CARD:
                    stageService.setRoundCard(
                            map.get(PostBackKeyName.STAGE.keyName),
                            userId,
                            Long.parseLong(map.get(PostBackKeyName.ROUND.keyName)),
                            map.get(PostBackKeyName.CARD.keyName));
                    break;

                case SET_THEME_CARD:
                    stageService.setThemeCard(
                            map.get(PostBackKeyName.STAGE.keyName),
                            userId,
                            Long.parseLong(map.get(PostBackKeyName.ROUND.keyName)),
                            map.get(PostBackKeyName.CARD.keyName),
                            map.get(PostBackKeyName.THEME_ANSWER.keyName));
                    break;

                case REQUEST_TO_FINISH_STAGE:
                    stageService.requestToFinishStage(userId);
                    break;

                case CONFIRM_TO_FINISH_STAGE:
                    String stageId_CONFIRM_TO_FINISH_ROUND = map.get(PostBackKeyName.STAGE.keyName);
                    stageService.confirmToFinishStage(userId, stageId_CONFIRM_TO_FINISH_ROUND);
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
