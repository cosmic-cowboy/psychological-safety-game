package com.slgerkamp.psychological.safety.game.domain.game.service;

import com.slgerkamp.psychological.safety.game.application.config.WebSocketConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class NotificationService {

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

///////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////
///////////////////  default method  //////////////////////
///////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////

    void publishToStompClient(String stageId){
        String subscriptionUrl = WebSocketConfig.DESTINATION_STAGE_PREFIX + "/" + stageId;
        simpMessagingTemplate.convertAndSend(
                subscriptionUrl,
                "hello");
    }
}
