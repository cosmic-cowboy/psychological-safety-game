package com.slgerkamp.psychological.safety.game.domain.game.service;

import com.slgerkamp.psychological.safety.game.application.config.WebSocketConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

///////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////
///////////////////  default method  //////////////////////
///////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////

    void publishToStompClient(String stageId){
        String subscriptionUrl = WebSocketConfig.DESTINATION_STAGE_PREFIX + "/" + stageId;
        log.debug("subscriptionUrl : " + subscriptionUrl);
        simpMessagingTemplate.convertAndSend(
                subscriptionUrl,
                "hello");
    }
}
