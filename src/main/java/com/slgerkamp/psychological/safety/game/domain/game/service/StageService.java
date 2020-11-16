package com.slgerkamp.psychological.safety.game.domain.game.service;


import com.linecorp.bot.model.action.PostbackAction;
import com.linecorp.bot.model.action.URIAction;
import com.linecorp.bot.model.message.FlexMessage;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.message.flex.component.Box;
import com.linecorp.bot.model.message.flex.component.Button;
import com.linecorp.bot.model.message.flex.component.Text;
import com.linecorp.bot.model.message.flex.container.Bubble;
import com.linecorp.bot.model.message.flex.unit.FlexLayout;
import com.linecorp.bot.model.message.flex.unit.FlexMarginSize;
import com.slgerkamp.psychological.safety.game.domain.game.*;
import com.slgerkamp.psychological.safety.game.infra.message.LineMessage;
import com.slgerkamp.psychological.safety.game.infra.message.ReplyToken;
import com.slgerkamp.psychological.safety.game.infra.model.*;
import com.slgerkamp.psychological.safety.game.infra.utils.CommonUtils;
import com.slgerkamp.psychological.safety.game.infra.utils.QrCodeGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class StageService {

    @Autowired
    private StageRepository stageRepository;

    @Autowired
    private StageMemberService stageMemberService;
    @Autowired
    private RoundService roundService;
    @Autowired
    private NotificationService notificationService;

    @Autowired
    private LineMessage lineMessage;
    @Autowired
    private MessageSource messageSource;
    @Autowired
    private QrCodeGenerator qrCodeGenerator;

    public void createStageTable(ReplyToken replyToken, String userId) {

        Optional<StageMember> optionalUserJoiningStageFromUserId =
                stageMemberService.getUserJoiningStageFromUserId(userId);


        // user does not join any stage or user join a stage as a participator
        if (!optionalUserJoiningStageFromUserId.isPresent()
                || optionalUserJoiningStageFromUserId.get().role.equals(StageMemberRole.PARTICIPATOR.name())) {

            // user join a stage as a participator and leave from this stage
            if (optionalUserJoiningStageFromUserId.isPresent()) {
                // deleteUser
                stageMemberService.leaveFromJoiningStage(userId, Collections.singletonList(optionalUserJoiningStageFromUserId.get()));
            }

            // createStage
            final Stage result_stage = createStageTable();
            stageMemberService.addCreator(userId, result_stage.id);

            qrCodeGenerator.createStageJoinUrlQrCode(result_stage.id);

            final String dashboardUrl = CommonUtils.createStageDashboardUrl(result_stage.id);
            final FlexMessage flexMessage = createSuccessFlexMessage(dashboardUrl);
            lineMessage.reply(replyToken, Collections.singletonList(flexMessage));

        // user join a stage as a creator
        } else {
            final FlexMessage flexMessage =
                    createConfirmToFinishStageFlexMessageForCreator(optionalUserJoiningStageFromUserId.get().stageId);
            lineMessage.reply(replyToken, Collections.singletonList(flexMessage));
        }
    }

    public Boolean requestToJoinStageForWeb(
            final String stageId,
            final OAuth2Authentication oAuth2Authentication) {
        Boolean isSuccess = false;
        // fetch stagesParticipantsWanted
        Optional<Stage> optionalStage = getParticipantsWantedStage(stageId);
        if (optionalStage.isPresent()) {
            Stage stage = optionalStage.get();
            stageMemberService.addMemberAndSendMessageToMemberFromWeb(oAuth2Authentication, stage);
            isSuccess = true;
        }
        return isSuccess;
    }

    public void startStage(String userId, String stageId) {
        Optional<Stage> optionalStage = getUserJoiningStageFromUserId(userId);

        // check sender joining stage existing & check this stage is PARTICIPANTS_WANTED
        if (optionalStage.isPresent() && optionalStage.get().id.equals(stageId)
                && optionalStage.get().status.equals(StageStatus.PARTICIPANTS_WANTED.name())) {
            // check everyone is friend with bot
            List<StageMember> stageMemberList = stageMemberService.getStageMemberForStage(stageId);
            if (!areFriendsWithBot(userId, stageMemberList)) {
                // if anyone is not a friend with bot, stop continuing the following process
                return;
            }

            // change stage status
            Stage currentStage = optionalStage.get();
            currentStage.status = StageStatus.START_GAME.name();
            currentStage.updateDate = Timestamp.valueOf(LocalDateTime.now());
            stageRepository.save(currentStage);

            // set cards for this stage
            boolean success = roundService.createNewRound(stageId);
            if (!success) {
                // because message sender is not stage creator
                finishStage(Optional.empty(), stageId);
            }
            // notify changing stage status to web
            notificationService.publishToStompClient(stageId);
        }
    }

    public void setRoundCard(ReplyToken replyToken, String stageId, String userId, Long roundId, String cardId) {
        boolean success =
                roundService.storeRoundCardOrThemeCardAndSendMessage(replyToken, userId, roundId, cardId, Optional.empty());
        notificationService.publishToStompClient(stageId);
        if (!success) {
            finishStage(Optional.of(replyToken), stageId);
        }
    }

    public void setThemeCard(ReplyToken replyToken, String stageId, String userId, Long roundId, String cardId, String themeAnswer) {
        boolean success =
                roundService.storeRoundCardOrThemeCardAndSendMessage(replyToken,userId, roundId, cardId, Optional.of(themeAnswer));
        notificationService.publishToStompClient(stageId);
        if (!success) {
            // because message sender is not stage creator
            finishStage(Optional.of(replyToken), stageId);
        }
    }

    public void confirmToFinishStage(ReplyToken replyToken, String userId, String stageId) {
        Optional<Stage> optionalStage = getUserJoiningStageFromUserId(userId);
        // check sender joining stage existing
        if (optionalStage.isPresent() && optionalStage.get().id.equals(stageId)) {
            finishStage(Optional.of(replyToken), stageId);
        } else {
            final String applyingStageNotFound = messageSource.getMessage(
                    "bot.stage.end.confirm.error",
                    null,
                    Locale.JAPANESE);
            lineMessage.reply(replyToken, Collections.singletonList(new TextMessage(applyingStageNotFound)));
        }
    }

    public Stage getStage(String stageId) {
        Optional<Stage> optionalStage = stageRepository.findById(stageId);
        return optionalStage.get();
    }

///////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////
///////////////////  private method  //////////////////////
///////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////

    private void finishStage(Optional<ReplyToken> optionalReplyToken, String stageId) {
        roundService.finishAllRounds(stageId);
        final Optional<Stage> optionalStage = stageRepository.findById(stageId);
        if (optionalStage.isPresent()) {

            Stage currentStage = optionalStage.get();
            currentStage.status = StageStatus.END_GAME.name();
            currentStage.updateDate = Timestamp.valueOf(LocalDateTime.now());
            stageRepository.save(currentStage);
            stageMemberService.terminatedStageMembers(stageId);

            final String successMessage = messageSource.getMessage(
                    "bot.stage.end.confirm.success",
                    null,
                    Locale.JAPANESE);
            optionalReplyToken.ifPresent(replyToken ->
                    lineMessage.reply(replyToken, Collections.singletonList(new TextMessage(successMessage))));

            notificationService.publishToStompClient(stageId);
        }
    }

    private Optional<Stage> getUserJoiningStageFromUserId(String userId) {
        List<StageMember> userStageMemberList = stageMemberService.getStageJoiningMembersFromUserId(userId);
        Optional<Stage> optionalStage = Optional.empty();
        if (userStageMemberList.size() > 0) {
            optionalStage = stageRepository.findById(userStageMemberList.get(0).stageId);
        }
        return optionalStage;
    }

    private Optional<Stage> getParticipantsWantedStage(String stageId) {
        // fetch stagesParticipantsWanted
        Optional<Stage> optionalStage = stageRepository.findById(stageId);
        if (optionalStage.isPresent()
                & (optionalStage.get().status.equals(StageStatus.PARTICIPANTS_WANTED.name()))) {
            return optionalStage;
        }
        return Optional.empty();
    }

    private Stage createStageTable() {
        final String stageId = CommonUtils.getUUID();
        final Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        final Stage stage = new Stage();
        stage.id = stageId;
        stage.createDate = now;
        stage.status = StageStatus.PARTICIPANTS_WANTED.name();
        stage.updateDate = now;
        return stageRepository.save(stage);
    }

    private boolean areFriendsWithBot(String userId, List<StageMember> stageMemberList) {
        boolean areFriendsWithBot = true;
        List<String> notFriendWithBotMemberList = new ArrayList<>();
        for (StageMember s : stageMemberList) {
            try {
                lineMessage.getProfile(s.userId);
            } catch (RuntimeException ex) {
                notFriendWithBotMemberList.add(s.userName);
            }
        }
        if (notFriendWithBotMemberList.size() > 0) {

            String notFriendWithBotMember = String.join("," , notFriendWithBotMemberList);
            final String notFriendWithBotMessage = messageSource.getMessage(
                    "bot.stage.start.confirm.error.notFriendWithBot",
                    new Object[]{notFriendWithBotMember},
                    Locale.JAPANESE);
            lineMessage.multicast(Collections.singleton(userId),
                    Collections.singletonList(new TextMessage(notFriendWithBotMessage)));
            areFriendsWithBot = false;
        }
        return areFriendsWithBot;
    }

///////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////
///////////////////  private method  //////////////////////
///////////////////  create message  //////////////////////
///////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////

    private FlexMessage createSuccessFlexMessage(String url) {
        String successMessage = messageSource.getMessage(
                "bot.stage.create.success",
                null,
                Locale.JAPANESE);
        String urlLabel = messageSource.getMessage(
                "bot.stage.create.success.label",
                null,
                Locale.JAPANESE);
        final Text textComponent =
                Text.builder()
                        .text(successMessage)
                        .wrap(true)
                        .margin(FlexMarginSize.XXL)
                        .build();
        final Button button =
                Button.builder()
                        .style(Button.ButtonStyle.PRIMARY)
                        .action(new URIAction(urlLabel, url, null))
                        .margin(FlexMarginSize.XXL)
                        .build();
        final Box bodyBox =
                Box.builder()
                        .layout(FlexLayout.VERTICAL)
                        .contents(Arrays.asList(textComponent))
                        .build();
        final Box footerBox =
                Box.builder()
                        .layout(FlexLayout.VERTICAL)
                        .contents(Arrays.asList(button))
                        .build();
        final Bubble bubble =
                Bubble.builder()
                        .body(bodyBox)
                        .footer(footerBox)
                        .build();
        return new FlexMessage(successMessage, bubble);
    }

    private FlexMessage createConfirmToFinishStageFlexMessageForCreator(String stageId) {
        final String buttonTitle = messageSource.getMessage(
                "bot.stage.end.request.title",
                new Object[]{stageId},
                Locale.JAPANESE);
        final String buttonText = messageSource.getMessage(
                "bot.stage.end.request.text",
                null,
                Locale.JAPANESE);
        final String postbackLabel = messageSource.getMessage(
                "bot.stage.end.request.button.label",
                null,
                Locale.JAPANESE);
        final String postbackText = messageSource.getMessage(
                "bot.stage.end.request.button.text",
                new Object[]{stageId},
                Locale.JAPANESE);
        final String postbackData = PostBackKeyName.ACTION.keyName + "="
                + PostBackAction.CONFIRM_TO_FINISH_STAGE.name() + "&"
                + PostBackKeyName.STAGE.keyName + "=" + stageId;
        final String altText = messageSource.getMessage(
                "bot.stage.end.request.altText",
                null,
                Locale.JAPANESE);

        final Text titleComponent =
                Text.builder()
                        .text(buttonTitle)
                        .wrap(true)
                        .weight(Text.TextWeight.BOLD)
                        .build();

        final Text textComponent =
                Text.builder()
                        .text(buttonText)
                        .wrap(true)
                        .margin(FlexMarginSize.XXL)
                        .build();

        final Button button =
                Button.builder()
                        .color("#dc3545")
                        .style(Button.ButtonStyle.PRIMARY)
                        .action(new PostbackAction(
                                postbackLabel, postbackData, postbackText))
                        .margin(FlexMarginSize.XXL)
                        .build();

        final Box bodyBox = Box.builder()
                .layout(FlexLayout.VERTICAL)
                .contents(Arrays.asList(titleComponent, textComponent))
                .build();

        final Box footerBox =
                Box.builder()
                        .layout(FlexLayout.VERTICAL)
                        .contents(Arrays.asList(button))
                        .build();

        final Bubble bubble =
                Bubble.builder()
                        .body(bodyBox)
                        .footer(footerBox)
                        .build();

        return new FlexMessage(altText, bubble);
    }
}
