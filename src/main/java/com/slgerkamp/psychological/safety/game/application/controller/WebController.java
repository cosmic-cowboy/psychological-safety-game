package com.slgerkamp.psychological.safety.game.application.controller;

import com.slgerkamp.psychological.safety.game.application.config.WebSocketConfig;
import com.slgerkamp.psychological.safety.game.application.model.RoundCardForView;
import com.slgerkamp.psychological.safety.game.domain.game.StageStatus;
import com.slgerkamp.psychological.safety.game.domain.game.service.RoundService;
import com.slgerkamp.psychological.safety.game.domain.game.service.StageMemberService;
import com.slgerkamp.psychological.safety.game.domain.game.service.StageService;
import com.slgerkamp.psychological.safety.game.infra.model.Stage;
import com.slgerkamp.psychological.safety.game.infra.model.StageMember;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
public class WebController {

    @Autowired
    private StageService stageService;
    @Autowired
    private StageMemberService stageMemberService;
    @Autowired
    private RoundService roundService;

    @Autowired
    private MessageSource messageSource;

    @GetMapping("/stage/{stageId}")
    public String stage(@PathVariable String stageId,
                        Model model,
                        final OAuth2Authentication oAuth2Authentication){
        // (common all methods in this class) get stage info and check stageMember or not
        Stage stage = stageService.getStage(stageId);
        List<StageMember> stageMemberList = stageMemberService.getStageMemberForDisplayStageMember(stage.id);
        boolean isMember = isMember(stageMemberList, oAuth2Authentication);

        if (!isMember) {
            stageService.requestToJoinStageForWeb(stage.id, oAuth2Authentication);
            // updated stage member list
            stageMemberList = stageMemberService.getStageMemberForDisplayStageMember(stage.id);
        }
        createModelForStage(model, stage, stageMemberList);
        return "stage";
    }

    @PostMapping("/stage/{stageId}/start")
    public void start(@PathVariable String stageId, final OAuth2Authentication oAuth2Authentication) {
        final String userId = getUserId(oAuth2Authentication);
        stageService.confirmToStartStage(userId, stageId);
    }

    @PostMapping("/stage/{stageId}/finish")
    public String finish(@PathVariable String stageId, final OAuth2Authentication oAuth2Authentication) {
        final String userId = getUserId(oAuth2Authentication);
        stageService.confirmToFinishStage(userId, stageId);
        return "stage";
    }


///////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////
///////////////////////  private method  //////////////////////////
///////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////

    private String getUserId(OAuth2Authentication oAuth2Authentication) {
        Map<String, Object> properties =
                (Map<String, Object>) oAuth2Authentication.getUserAuthentication().getDetails();
        return (String) properties.get("userId");
    }

    private boolean isMember(List<StageMember> stageMemberList, final OAuth2Authentication oAuth2Authentication) {

        Map<String, Object> properties = (Map<String, Object>) oAuth2Authentication.getUserAuthentication().getDetails();
        final String userId = (String) properties.get("userId");
        boolean isMember = false;
        for(StageMember stageMember : stageMemberList) {
            if (stageMember.userId.equals(userId)) {
                isMember = true;
            }
        }
        return isMember;
    }

    private void createModelForStage(Model model, Stage stage, List<StageMember> stageMemberList) {
        Map<Long, List<RoundCardForView>> roundCardForViewMap = roundService.getRoundCards(stage.id);
        Map<String, Map<String, List<String>>> roundRetrospectiveMap = roundService.getRoundRetrospective(stage.id);
        String subscriptionUrl = WebSocketConfig.DESTINATION_STAGE_PREFIX + "/" + stage.id;
        boolean stageNotStartedYet = stage.status.equals(StageStatus.PARTICIPANTS_WANTED.name());

        final String webStageTitlePrefix = messageSource.getMessage(
                "web.stage.title.prefix",
                null,
                Locale.JAPANESE);

        model.addAttribute("stageNotStartedYet", stageNotStartedYet);
        model.addAttribute("stageTitle", webStageTitlePrefix + stage.id);
        model.addAttribute("stageQRcode", "/stage/" + stage.id + "/qrcode");
        model.addAttribute("stageStatus", stage.status);
        model.addAttribute("stageMemberList", stageMemberList);
        model.addAttribute("roundCardForViewMap", roundCardForViewMap);
        model.addAttribute("roundRetrospectiveMap", roundRetrospectiveMap);
        model.addAttribute("subscriptionUrl", subscriptionUrl);
        model.addAttribute("startPostUrl", "/stage/" + stage.id + "/start");
        model.addAttribute("finishPostUrl", "/stage/" + stage.id + "/finish");

        for(StageMember stageMember : stageMemberList){
            model.addAttribute(stageMember.userId, stageMember);
        }
    }
}
