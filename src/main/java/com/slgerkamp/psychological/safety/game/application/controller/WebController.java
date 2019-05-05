package com.slgerkamp.psychological.safety.game.application.controller;

import com.slgerkamp.psychological.safety.game.domain.game.service.StageService;
import com.slgerkamp.psychological.safety.game.infra.model.RoundCard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class WebController {

    @Autowired
    private StageService stageService;

    @GetMapping("/stage/{stageId}")
    public String stage(@PathVariable String stageId){
        List<RoundCard> roundCardList = stageService.getRoundCards(stageId);
        return "stage";
    }

}
