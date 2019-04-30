package com.slgerkamp.psychological.safety.game.application.controller;

import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.PostbackEvent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.spring.boot.annotation.EventMapping;
import com.linecorp.bot.spring.boot.annotation.LineMessageHandler;
import com.slgerkamp.psychological.safety.game.domain.game.*;
import com.slgerkamp.psychological.safety.game.domain.game.service.StageService;
import com.slgerkamp.psychological.safety.game.infra.model.StageMember;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@LineMessageHandler
public class GameController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GameController.class);

    @Autowired
    private StageService stageService;

    @EventMapping
    public void handleTextMessageEvent(MessageEvent<TextMessageContent> event) {

        final String userId = event.getSource().getUserId();
        final String message = event.getMessage().getText();

        final Optional<StageMember> optionalStageMember = stageService.getStageMember(userId);

        if (optionalStageMember.isPresent()) {
            StageMemberStatus stageMemberStatus =
                    StageMemberStatus.valueOf(optionalStageMember.get().status);
            switch (stageMemberStatus) {
                case APPLY_TO_JOIN :
                    stageService.confirmPasswordToJoinAStage(userId, message);
                    break;
                case JOINING:
                    break;
                default:
                    break;
            }
        }
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
                    stageService.createStage(userId);
                    break;

                case GET_STAGES:
                    stageService.getStagesParticipantsWanted(userId);
                    break;

                case REQUEST_TO_JOIN_STAGE:
                    String stageId_REQUEST_TO_JOIN_STAGE = map.get(PostBackKeyName.STAGE.keyName);
                    stageService.requestToJoinStage(userId, stageId_REQUEST_TO_JOIN_STAGE);
                    break;

                case REQUEST_TO_START_STAGE:
                    stageService.requestToStartStage(userId);
                    break;

                case CONFIRM_TO_START_STAGE:
                    String stageId_CONFIRM_TO_START_ROUND = map.get(PostBackKeyName.STAGE.keyName);
                    stageService.confirmToStartStage(userId, stageId_CONFIRM_TO_START_ROUND);
                    break;

                case SET_ROUND_CARD:
                    String stageId = map.get(PostBackKeyName.STAGE.keyName);
                    String roundId = map.get(PostBackKeyName.ROUND.keyName);
                    String cardId = map.get(PostBackKeyName.CARD.keyName);
                    stageService.setRoundCard(userId, roundId, cardId);
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
