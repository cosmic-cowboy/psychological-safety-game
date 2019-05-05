package com.slgerkamp.psychological.safety.game.application.controller;

import com.slgerkamp.psychological.safety.game.domain.game.service.StageService;
import com.slgerkamp.psychological.safety.game.infra.model.RoundCard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

@RestController
public class APIController {

    @Autowired
    private StageService stageService;

    @GetMapping("/stage/{stageId}/check/{latestRoundCard}")
    public Map<String, Object> checkStage(@PathVariable String stageId, @PathVariable Long latestRoundCard) {
        Boolean needUpdate = false;
        Long millSecondOfLatestRoundCard = stageService.getMillSecondOfLatestRoundCard(stageId);
        if (millSecondOfLatestRoundCard > latestRoundCard) {
            needUpdate = true;
        }
        final Map<String, Object> map = new HashMap<>();
        map.put("needUpdate", needUpdate);
        return map;
    }

}
