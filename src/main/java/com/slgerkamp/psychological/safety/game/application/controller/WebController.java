package com.slgerkamp.psychological.safety.game.application.controller;

import com.slgerkamp.psychological.safety.game.domain.game.service.StageService;
import com.slgerkamp.psychological.safety.game.infra.model.RoundCard;
import com.slgerkamp.psychological.safety.game.infra.model.Stage;
import com.slgerkamp.psychological.safety.game.infra.model.StageMember;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

@Controller
public class WebController {

    @Autowired
    private StageService stageService;

    @GetMapping("/stage/{stageId}")
    public String stage(@PathVariable String stageId, Model model){
        Stage stage = stageService.getStage(stageId);
        Map<Long, List<RoundCard>> roundCardMap = stageService.getRoundCards(stageId);
        List<StageMember> stageMemberList = stageService.getStageMemberForStage(stageId);
        Long millSecondOfLatestRoundCard = stageService.getMillSecondOfLatestRoundCard(stageId);
        String urlForPolling = "/stage/" + stageId + "/check/" + millSecondOfLatestRoundCard;

        model.addAttribute("stageTitle", stage.id);
        model.addAttribute("stageMemberList", stageMemberList);
        model.addAttribute("roundCardMap", roundCardMap);
        model.addAttribute("urlForPolling", urlForPolling);

        for(StageMember stageMember : stageMemberList){
            model.addAttribute(stageMember.userId, stageMember);
        }
        return "stage";
    }

}
