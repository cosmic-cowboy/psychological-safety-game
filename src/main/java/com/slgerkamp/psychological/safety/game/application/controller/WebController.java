package com.slgerkamp.psychological.safety.game.application.controller;

import com.slgerkamp.psychological.safety.game.application.config.WebSocketConfig;
import com.slgerkamp.psychological.safety.game.application.form.StageJoinForm;
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
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import javax.validation.Valid;
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

        if (isMember) {
            createModelForStage(model, stage, stageMemberList);
            return "stage";
        } else {
            return "redirect:/stage/" + stage.id + "/join";
        }
    }

    @GetMapping("/stage/{stageId}/join")
    public String stageJoinInit(@PathVariable String stageId,
                                StageJoinForm stageJoinForm,
                                Model model,
                                final OAuth2Authentication oAuth2Authentication){
        // (common all methods in this class) get stage info and check stageMember or not
        Stage stage = stageService.getStage(stageId);
        List<StageMember> stageMemberList = stageMemberService.getStageMemberForStage(stage.id);
        boolean isMember = isMember(stageMemberList, oAuth2Authentication);

        if (isMember) {
            return "redirect:/stage/" + stage.id;
        } else {
            createModelForJoinForm(model, stage);
            return "stageJoin";
        }
    }

    @PostMapping("/stage/{stageId}/join")
    public String stageJoinSubmit(@PathVariable String stageId,
                                  @Valid StageJoinForm stageJoinForm,
                                  BindingResult bindingResult,
                                  Model model,
                                  final OAuth2Authentication oAuth2Authentication) {
        // (common all methods in this class) get stage info and check stageMember or not
        Stage stage = stageService.getStage(stageId);
        List<StageMember> stageMemberList = stageMemberService.getStageMemberForStage(stage.id);
        boolean isMember = isMember(stageMemberList, oAuth2Authentication);

        if (isMember) {
            return "redirect:/stage/" + stage.id;
        } else if (bindingResult.hasErrors()) {
            // return error
        } else {
            String password = stageJoinForm.getInputNumber();
            Boolean isSuccess = stageService.requestToJoinStageForWeb(stage.id, oAuth2Authentication, password);
            if (isSuccess) {
                return "redirect:/stage/" + stage.id;
            } else {
                bindingResult.rejectValue("inputNumber", null, "値が正しくありません");
                // return error
            }
        }
        // return error
        createModelForJoinForm(model, stage);
        return "stageJoin";
    }

///////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////
///////////////////////  private method  //////////////////////////
///////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////

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
        model.addAttribute("stagePassword", stage.password);
        model.addAttribute("stageStatus", stage.status);
        model.addAttribute("stageMemberList", stageMemberList);
        model.addAttribute("roundCardForViewMap", roundCardForViewMap);
        model.addAttribute("roundRetrospectiveMap", roundRetrospectiveMap);
        model.addAttribute("subscriptionUrl", subscriptionUrl);

        for(StageMember stageMember : stageMemberList){
            model.addAttribute(stageMember.userId, stageMember);
        }
    }

    private void createModelForJoinForm(Model model, Stage stage) {
        final String joinRequestTitle = messageSource.getMessage(
                "web.stage.join.request.title",
                new Object[]{stage.id},
                Locale.JAPANESE);
        final String joinRequest = messageSource.getMessage(
                "web.stage.join.request",
                new Object[]{stage.id},
                Locale.JAPANESE);
        final String joinRequestButton = messageSource.getMessage(
                "web.stage.join.request.button",
                new Object[]{stage.id},
                Locale.JAPANESE);
        final String joinRequestPlaceholder = messageSource.getMessage(
                "web.stage.join.request.placeholder",
                new Object[]{stage.id},
                Locale.JAPANESE);
        final String joinRequestWrongPassword = messageSource.getMessage(
                "web.stage.join.request.wrong.password",
                new Object[]{stage.id},
                Locale.JAPANESE);

        model.addAttribute("stageId", stage.id);
        model.addAttribute("postUrl", "/stage/" + stage.id + "/join");
        model.addAttribute("joinRequestTitle", joinRequestTitle);
        model.addAttribute("joinRequest", joinRequest);
        model.addAttribute("joinRequestButton", joinRequestButton);
        model.addAttribute("joinRequestPlaceholder", joinRequestPlaceholder);
        model.addAttribute("joinRequestWrongPassword", joinRequestWrongPassword);
    }
}
