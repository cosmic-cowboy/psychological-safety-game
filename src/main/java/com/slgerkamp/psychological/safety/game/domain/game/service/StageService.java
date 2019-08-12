package com.slgerkamp.psychological.safety.game.domain.game.service;


import com.linecorp.bot.model.action.PostbackAction;
import com.linecorp.bot.model.action.URIAction;
import com.linecorp.bot.model.message.FlexMessage;
import com.linecorp.bot.model.message.TemplateMessage;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.message.flex.component.Box;
import com.linecorp.bot.model.message.flex.component.Button;
import com.linecorp.bot.model.message.flex.component.Separator;
import com.linecorp.bot.model.message.flex.component.Text;
import com.linecorp.bot.model.message.flex.container.Bubble;
import com.linecorp.bot.model.message.flex.container.Carousel;
import com.linecorp.bot.model.message.flex.unit.FlexAlign;
import com.linecorp.bot.model.message.flex.unit.FlexFontSize;
import com.linecorp.bot.model.message.flex.unit.FlexLayout;
import com.linecorp.bot.model.message.flex.unit.FlexMarginSize;
import com.linecorp.bot.model.message.template.*;
import com.linecorp.bot.model.profile.UserProfileResponse;
import com.slgerkamp.psychological.safety.game.application.config.WebSocketConfig;
import com.slgerkamp.psychological.safety.game.application.controller.GameController;
import com.slgerkamp.psychological.safety.game.application.model.RoundCardForView;
import com.slgerkamp.psychological.safety.game.domain.game.*;
import com.slgerkamp.psychological.safety.game.infra.message.LineMessage;
import com.slgerkamp.psychological.safety.game.infra.model.*;
import com.slgerkamp.psychological.safety.game.infra.utils.CommonUtils;
import com.slgerkamp.psychological.safety.game.infra.utils.QrCodeGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class StageService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GameController.class);


    @Autowired
    private StageRepository stageRepository;

    @Autowired
    private StageMemberRepository stageMemberRepository;

    @Autowired
    private RoundRepository roundRepository;

    @Autowired
    private RoundCardRepository roundCardRepository;

    @Autowired
    private StageUserCardRepository stageUserCardRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private LineMessage lineMessage;

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private QrCodeGenerator qrCodeGenerator;

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    public void createStageTable(String userId) {
        // check whether sender already join a stage or not
        if (!userAlreadyJoinedStage(userId)) {

            final Stage result_stage = createStageTable();

            addMember(userId, result_stage.id);

            final String url = CommonUtils.createStageUrl(result_stage.id);

            qrCodeGenerator.create(url, result_stage.id);

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

            lineMessage.multicast(Collections.singleton(userId), Collections.singletonList(new FlexMessage(successMessage, bubble)));
        }
    }

    public void getStagesParticipantsWanted(String userId) {
        // check whether sender already join a stage or not
        if (!userAlreadyJoinedStage(userId)) {

            // fetch stagesParticipantsWanted
            final List<Stage> stageList = stageRepository.findParticipantsWantedStageList(PageRequest.of(0, 10));
            log.debug(stageList.stream().map(n -> n.id).collect(Collectors.joining(",")));
            // send them to sender and ask which stage sender want to join
            if (stageList.size() > 0) {
                TemplateMessage templateMessage = createParticipantsWantedStageList(stageList);
                lineMessage.multicast(Collections.singleton(userId), Collections.singletonList(templateMessage));
            } else {
                String msg = messageSource.getMessage(
                        "bot.stage.join.request.no.stage",
                        null,
                        Locale.JAPANESE);
                lineMessage.multicast(Collections.singleton(userId), Collections.singletonList(new TextMessage(msg)));
            }
        }
    }

    public Boolean requestToJoinStageForWeb(String stageId, final OAuth2Authentication oAuth2Authentication, String password) {
        Boolean isSuccess = false;
        // already checked whether sender already join a stage or not

        // fetch stagesParticipantsWanted
        Optional<Stage> optionalStage = getParticipantsWantedStage(stageId);
        if (optionalStage.isPresent()) {
            Stage stage = optionalStage.get();
            if (stage.password.equals(password)) {
                addMemberAndSendMessageToMemberFromWeb(oAuth2Authentication, stage);
                isSuccess = true;
            }
        }
        return isSuccess;
    }

    public void requestToJoinStage(String userId, String stageId) {
        // check whether sender already join a stage or not
        if (!userAlreadyJoinedStage(userId)) {

            // fetch stagesParticipantsWanted
            Optional<Stage> optionalStage = getParticipantsWantedStage(stageId);

            if (optionalStage.isPresent()) {
                addTempMember(userId, stageId);
                final String joinConfirm = messageSource.getMessage(
                        "bot.stage.join.confirm",
                        new Object[]{stageId},
                        Locale.JAPANESE);
                lineMessage.multicast(Collections.singleton(userId),
                        Collections.singletonList(new TextMessage(joinConfirm)));
            } else {
                final String cannotJoinForSomeReason = messageSource.getMessage(
                        "bot.stage.join.confirm.error",
                        null,
                        Locale.JAPANESE);
                lineMessage.multicast(Collections.singleton(userId),
                        Collections.singletonList(new TextMessage(cannotJoinForSomeReason)));
            }
        }
    }

    public void confirmPasswordToJoinAStage(String userId, String password) {
        if (password.length() != 6) {
        } else {
            // check whether sender already join a stage or not
            if (!userAlreadyJoinedStage(userId)) {
                // check the stage that sender wants to join
                Optional<StageMember> optionalStageMember = getApplyingStage(userId);

                if (optionalStageMember.isPresent()) {
                    StageMember stageMember = optionalStageMember.get();
                    Optional<Stage> optionalStage = getParticipantsWantedStage(stageMember.stageId);

                    if (optionalStage.isPresent()) {
                        Stage stage = optionalStage.get();
                        if (stage.password.equals(password)) {
                            addMemberAndSendMessageToMember(userId, stage);
                        } else {
                            final String wrongPassword = messageSource.getMessage(
                                    "bot.stage.input.password.wrong.password",
                                    new Object[]{stage.id},
                                    Locale.JAPANESE);
                            lineMessage.multicast(Collections.singleton(userId),
                                    Collections.singletonList(new TextMessage(wrongPassword)));
                        }
                    }
                }
            }
        }
    }

    public void requestToStartStage(String userId) {

        List<StageMember> userStageMemberList =
                stageMemberRepository.findByUserIdAndStatus(userId, StageMemberStatus.JOINING.name());
        Optional<Stage> optionalStage = Optional.empty();
        if (userStageMemberList.size() > 0) {
            optionalStage = stageRepository.findById(userStageMemberList.get(0).stageId);
        }

        // check sender joined any stage
        // check this stage is PARTICIPANTS_WANTED
        if (userStageMemberList.size() > 0
                && optionalStage.isPresent()
                && optionalStage.get().status.equals(StageStatus.PARTICIPANTS_WANTED.name())) {
            final Stage stage = optionalStage.get();

            final String buttonTitle = messageSource.getMessage(
                    "bot.stage.start.request.title",
                    new Object[]{stage.id},
                    Locale.JAPANESE);
            final String buttonText = messageSource.getMessage(
                    "bot.stage.start.request.text",
                    null,
                    Locale.JAPANESE);

            final String postbackLabel = messageSource.getMessage(
                    "bot.stage.start.request.button.label",
                    null,
                    Locale.JAPANESE);
            final String postbackText = messageSource.getMessage(
                    "bot.stage.start.request.button.text",
                    new Object[]{stage.id},
                    Locale.JAPANESE);
            final String postbackData = PostBackKeyName.ACTION.keyName + "="
                    + PostBackAction.CONFIRM_TO_START_STAGE.name() + "&"
                    + PostBackKeyName.STAGE.keyName + "=" + stage.id;

            final String altText = messageSource.getMessage(
                    "bot.stage.start.request.altText",
                    null,
                    Locale.JAPANESE);

            final FlexMessage flexMessage =
                    createFlexButton(buttonTitle, buttonText, postbackLabel, postbackText, postbackData, altText);
            lineMessage.multicast(Collections.singleton(userId), Collections.singletonList(flexMessage));

        } else {
            final String applyingStageNotFound = messageSource.getMessage(
                    "bot.stage.start.request.error",
                    null,
                    Locale.JAPANESE);
            lineMessage.multicast(Collections.singleton(userId),
                    Collections.singletonList(new TextMessage(applyingStageNotFound)));

        }
    }


    public void confirmToStartStage(String userId, String stageId) {
        // check sender joined this stage
        // check this stage is PARTICIPANTS_WANTED

        List<StageMember> userStageMemberList =
                stageMemberRepository.findByUserIdAndStatusAndStageId(
                        userId, StageMemberStatus.JOINING.name(), stageId);
        Optional<Stage> optionalStage = Optional.empty();
        if (userStageMemberList.size() > 0) {
            optionalStage = stageRepository.findById(stageId);
        }

        if (userStageMemberList.size() > 0
                && optionalStage.isPresent()
                && optionalStage.get().status.equals(StageStatus.PARTICIPANTS_WANTED.name())) {

            // check everyone is friend with bot
            List<StageMember> stageMemberList = stageMemberRepository.findByStageId(stageId);
            List<String> notFriendWithBotMemberList = new ArrayList<>();
            for (StageMember s : stageMemberList) {
                log.debug("confirmToStartStage start 心理的安全性ゲームBot");
                try {
                    lineMessage.getProfile(s.userId);
                } catch (RuntimeException ex) {
                    log.debug("confirmToStartStage RuntimeException 心理的安全性ゲームBot");
                    notFriendWithBotMemberList.add(s.userName);
                }
            }
            if (notFriendWithBotMemberList.size() > 0) {
                log.debug("notFriendWithBotMemberList : " + notFriendWithBotMemberList);

                String notFriendWithBotMember = String.join("," , notFriendWithBotMemberList);
                final String notFriendWithBotMessage = messageSource.getMessage(
                        "bot.stage.start.confirm.error.notFriendWithBot",
                        new Object[]{notFriendWithBotMember},
                        Locale.JAPANESE);
                lineMessage.multicast(Collections.singleton(userId),
                        Collections.singletonList(new TextMessage(notFriendWithBotMessage)));
                return;
            }

            Collections.shuffle(stageMemberList);

            // change stage status
            Stage currentStage = optionalStage.get();
            currentStage.status = StageStatus.START_GAME.name();
            currentStage.updateDate = Timestamp.valueOf(LocalDateTime.now());
            stageRepository.save(currentStage);

            publishToStompClient(stageId);

            // set cards for this stage
            List<Card> commentsCards = cardRepository.findByType(CardType.COMMENT.name());
            List<Card> optionCards = cardRepository.findByType(CardType.OPTION.name());
            Collections.shuffle(commentsCards);
            Collections.shuffle(optionCards);
            int count = commentsCards.size() / stageMemberList.size();
            if (count > stageMemberList.size()) {
                count = stageMemberList.size();
            }
            List<StageUserCard> stageUserCardList = new ArrayList<>();

            for (int outer = 0; outer < stageMemberList.size(); outer++) {
                stageMemberList.get(outer).turnNumber = outer;
                final String stageMemberUserId = stageMemberList.get(outer).userId;
                final String stageMemberStageId = stageMemberList.get(outer).stageId;

                for (int inner = 0; inner < count; inner++) {
                    StageUserCard commentStageUserCard = createStageUserCard(
                            stageMemberUserId, stageMemberStageId,
                            commentsCards.get((outer * count) + inner));
                    stageUserCardList.add(commentStageUserCard);
                }

                StageUserCard optionStageUserCard = createStageUserCard(
                        stageMemberUserId, stageMemberStageId,
                        optionCards.get(outer));
                stageUserCardList.add(optionStageUserCard);
            }

            stageMemberRepository.saveAll(stageMemberList);
            stageUserCardRepository.saveAll(stageUserCardList);

            // set turn_number for this stage
            // set cards for this stage

            createNewRound(stageId);

        } else {
            final String applyingStageNotFound = messageSource.getMessage(
                    "bot.stage.start.confirm.error",
                    null,
                    Locale.JAPANESE);
            lineMessage.multicast(Collections.singleton(userId),
                    Collections.singletonList(new TextMessage(applyingStageNotFound)));

        }
    }

    public void setRoundCard(String userId, Long roundId, String cardId) {
        // check current turn
        Optional<Round> optionalRound = roundRepository.findById(roundId);
        Card card = cardRepository.findById(cardId).get();
        List<StageMember> userStageMemberList =
                stageMemberRepository.findByUserIdAndStatus(userId, StageMemberStatus.JOINING.name());

        if (optionalRound.isPresent() && userStageMemberList.size() > 0) {
            StageMember userStageMember = userStageMemberList.get(0);
            Round round = optionalRound.get();

            Integer turnNumber = userStageMember.turnNumber;

            if (round.currentTurnNumber == turnNumber) {

                RoundCard roundCard = new RoundCard();
                roundCard.id = CommonUtils.getUUID();
                roundCard.roundId = roundId;
                roundCard.userId = userId;
                roundCard.cardId = card.id;
                roundCard.turnNumber = turnNumber;
                roundCard.createDate = Timestamp.valueOf(LocalDateTime.now());
                roundCardRepository.save(roundCard);
                stageUserCardRepository.deleteByStageIdAndUserIdAndCardId(round.stageId, userId, card.id);

                final String successMessage = messageSource.getMessage(
                        "bot.round.set.round.card.success",
                        null,
                        Locale.JAPANESE);
                lineMessage.multicast(Collections.singleton(userId),
                        Collections.singletonList(new TextMessage(successMessage)));

                // Go to next turn
                List<StageMember> stageMemberList = stageMemberRepository.findByStageId(round.stageId);

                Integer nextCurrentTurnNumber;
                if(!card.type.equals(CardType.THEME.name())
                        && !card.type.equals(CardType.OPTION.name())) {

                    if (round.currentTurnNumber == (stageMemberList.size() - 1)) {
                        nextCurrentTurnNumber = 0;
                    } else {
                        nextCurrentTurnNumber = round.currentTurnNumber + 1;
                    }
                    round.currentTurnNumber = nextCurrentTurnNumber;
                    roundRepository.save(round);
                } else {

                    nextCurrentTurnNumber = round.currentTurnNumber;
                }

                // next action
                String nextUserId = stageMemberList
                        .stream()
                        .filter(s -> s.turnNumber == nextCurrentTurnNumber)
                        .map(s -> s.userId)
                        .findFirst().get();

                if(nextCurrentTurnNumber != round.currentRoundNumber) {
                    final String nextYourTurnMessage = messageSource.getMessage(
                            "bot.round.set.round.card.next.your.turn",
                            null,
                            Locale.JAPANESE);
                    lineMessage.multicast(Collections.singleton(nextUserId),
                            Collections.singletonList(new TextMessage(nextYourTurnMessage)));
                } else {

                    if (card.type.equals(CardType.THEME.name())) {
                        createNewRound(round.stageId);
                    } else {
                        List<Card> themeList = cardRepository.findByType(CardType.THEME.name());
                        createCard(round.stageId, round.id, themeList, nextUserId);
                        final String nextYourTurnMessage = messageSource.getMessage(
                                "bot.round.set.round.card.for.theme.next.your.turn",
                                null,
                                Locale.JAPANESE);
                        lineMessage.multicast(Collections.singleton(nextUserId),
                                Collections.singletonList(new TextMessage(nextYourTurnMessage)));
                    }
                }

                publishToStompClient(round.stageId);
            } else {
                final String notYourTurnMessage = messageSource.getMessage(
                        "bot.round.set.round.card.not.your.turn",
                        null,
                        Locale.JAPANESE);
                lineMessage.multicast(Collections.singleton(userId),
                        Collections.singletonList(new TextMessage(notYourTurnMessage)));
            }
        } else {
            final String errorMessage = messageSource.getMessage(
                    "bot.round.set.round.card.error",
                    null,
                    Locale.JAPANESE);
            lineMessage.multicast(Collections.singleton(userId),
                    Collections.singletonList(new TextMessage(errorMessage)));
        }
    }

    public void requestToFinishStage(String userId) {
        List<StageMember> userStageMemberList =
                stageMemberRepository.findByUserIdAndStatus(userId, StageMemberStatus.JOINING.name());
        Optional<Stage> optionalStage = Optional.empty();
        if (userStageMemberList.size() > 0 ) {
            optionalStage = stageRepository.findById(userStageMemberList.get(0).stageId);
        }

        // check sender joined any stage
        if (userStageMemberList.size() > 0
                && optionalStage.isPresent()) {
            final Stage stage = optionalStage.get();
            final String buttonTitle = messageSource.getMessage(
                    "bot.stage.end.request.title",
                    new Object[]{stage.id},
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
                    new Object[]{stage.id},
                    Locale.JAPANESE);
            final String postbackData = PostBackKeyName.ACTION.keyName + "="
                    + PostBackAction.CONFIRM_TO_FINISH_STAGE.name() + "&"
                    + PostBackKeyName.STAGE.keyName + "=" + stage.id;
            final String altText = messageSource.getMessage(
                    "bot.stage.end.request.altText",
                    null,
                    Locale.JAPANESE);

            final FlexMessage flexMessage =
                    createFlexButton(buttonTitle, buttonText, postbackLabel, postbackText, postbackData, altText);

            lineMessage.multicast(Collections.singleton(userId), Collections.singletonList(flexMessage));

        } else {
            final String applyingStageNotFound = messageSource.getMessage(
                    "bot.stage.end.request.error",
                    null,
                    Locale.JAPANESE);
            lineMessage.multicast(Collections.singleton(userId),
                    Collections.singletonList(new TextMessage(applyingStageNotFound)));

        }
    }

    public void confirmToFinishStage(String userId, String stageId) {
        List<StageMember> userStageMemberList =
                stageMemberRepository.findByUserIdAndStatus(userId, StageMemberStatus.JOINING.name());
        Optional<Stage> optionalStage = Optional.empty();
        if (userStageMemberList.size() > 0 ) {
            optionalStage = stageRepository.findById(userStageMemberList.get(0).stageId);
        }

        if (userStageMemberList.size() > 0
                && optionalStage.isPresent()
                && optionalStage.get().id.equals(stageId)) {

            finishStage(stageId);

        } else {
            final String applyingStageNotFound = messageSource.getMessage(
                    "bot.stage.end.confirm.error",
                    null,
                    Locale.JAPANESE);
            lineMessage.multicast(Collections.singleton(userId),
                    Collections.singletonList(new TextMessage(applyingStageNotFound)));

        }
    }


    private FlexMessage createFlexButton(
            String buttonTitle,
            String buttonText,
            String postbackLabel,
            String postbackText,
            String postbackData,
            String altText) {
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
                        .style(Button.ButtonStyle.PRIMARY)
                        .action(new PostbackAction(postbackLabel, postbackData, postbackText))
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


//////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////
///////////////////  public method  //////////////////////
//////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////

    public Stage getStage(String stageId) {
        Optional<Stage> optionalStage = stageRepository.findById(stageId);
        return optionalStage.get();
    }

//    public Long getMillSecondOfLatestUpdate(String stageId){
//        Long millSecondOfLatestUpdate = Long.valueOf(0);
//
//        List<Round> roundList = roundRepository.findByStageIdOrderByCreateDateDesc(stageId);
//        if (roundList.size() > 0) {
//            List<Long> roundIdList = roundList.stream().map(r -> r.id).collect(Collectors.toList());
//            Optional<RoundCard> optionalRoundCard =
//                    roundCardRepository.findFirstByRoundIdInOrderByCreateDateDesc(roundIdList);
//            if (optionalRoundCard.isPresent()) {
//                millSecondOfLatestUpdate = optionalRoundCard.get().createDate.getTime();
//                log.debug("millSecondOfLatestRoundCard : " + millSecondOfLatestUpdate);
//            }
//        }
//
//        if (millSecondOfLatestUpdate == 0) {
//            final Optional<Stage> optionalStage = stageRepository.findById(stageId);
//            if (optionalStage.isPresent()) {
//                millSecondOfLatestUpdate = optionalStage.get().updateDate.getTime();
//                log.debug("millSecondOfLatestRoundCard : " + millSecondOfLatestUpdate);
//            }
//
//            final Optional<StageMember> optionalStageMember =
//                    stageMemberRepository.findFirstByStageIdOrderByCreateDateDesc(stageId);
//            if (optionalStageMember.isPresent()) {
//                Long createDate = optionalStageMember.get().createDate.getTime();
//                if (millSecondOfLatestUpdate < createDate) {
//                    millSecondOfLatestUpdate = createDate;
//                }
//            }
//        }
//
//        return millSecondOfLatestUpdate;
//    }

    public Map<Long, List<RoundCardForView>> getRoundCards(String stageId) {
        List<Round> roundList = roundRepository.findByStageIdOrderByCreateDateDesc(stageId);
        Map<Long, List<RoundCardForView>> roundCardMap = new TreeMap<>();

        if (roundList.size() > 0) {
            final List<Long> roundIdList = roundList.stream().map(r -> r.id).collect(Collectors.toList());
            final List<RoundCard> roundCardList = roundCardRepository.findByRoundIdIn(roundIdList);
            final List<String> cardIdList = roundCardList.stream().map(r -> r.cardId).collect(Collectors.toList());
            final List<Card> cardList = cardRepository.findByIdIn(cardIdList);

            List<RoundCardForView> roundCardForViewList = new ArrayList<>();
            for(RoundCard roundCard : roundCardList) {
                for (Card tempCard : cardList) {
                    if (tempCard.id.equals(roundCard.cardId)){
                        Card card = tempCard;
                        RoundCardForView roundCardForView = new RoundCardForView();
                        roundCardForView.id = roundCard.id;
                        roundCardForView.roundId = roundCard.roundId;
                        roundCardForView.turnNumber = roundCard.turnNumber;
                        roundCardForView.userId = roundCard.userId;
                        roundCardForView.cardId = roundCard.cardId;
                        roundCardForView.type = card.type;
                        roundCardForView.typeForDisplay = messageSource.getMessage(
                                CardType.valueOf(card.type).message,
                                null,
                                Locale.JAPANESE);
                        roundCardForView.title = card.title;
                        roundCardForView.text = card.text;
                        roundCardForView.word = roundCard.word;
                        roundCardForViewList.add(roundCardForView);
                    }
                }
            }
            Map<Long, List<RoundCardForView>> hashRoundCardMap =
                    roundCardForViewList.stream().collect(Collectors.groupingBy(r -> r.roundId));
            log.debug("hashRoundCardMap : " + hashRoundCardMap);
            roundCardMap = new TreeMap<>(hashRoundCardMap).descendingMap();

        }
        log.debug("roundCardMap : " + roundCardMap);
        return roundCardMap;
    }

    public List<StageMember> getStageMemberForStage(String stageId) {
        List<StageMember> stageMemberList = stageMemberRepository.findByStageId(stageId);
        return stageMemberList;
    }

    public List<StageMember> getStageMemberForSisplayStageMember(String stageId) {
        List<String> statusList = Arrays.asList(
                StageMemberStatus.JOINING.name(),
                StageMemberStatus.TERMINATED.name());
        List<StageMember> stageMemberList = stageMemberRepository.findByStageIdAndStatusIn(stageId, statusList);
        return stageMemberList;
    }

    public Optional<StageMember> getStageMemberForEachUser(String userId) {
        Optional<StageMember> optionalStageMember = Optional.empty();
        List<String> statusList = Arrays.asList(
                StageMemberStatus.APPLY_TO_JOIN.name(),
                StageMemberStatus.JOINING.name());
        List<StageMember> stageMemberList = stageMemberRepository.findByUserIdAndStatusIn(userId, statusList);
        if (stageMemberList.size() > 0) {
            optionalStageMember = Optional.of(stageMemberList.get(0));
        }
        return optionalStageMember;
    }

//////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////
///////////////////////  round  //////////////////////////
//////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////


    private void createNewRound(String stageId){
        // finish All Rounds just in case and get All rounds
        List<Round> roundList = finishAllRounds(stageId);
        // get stage member list
        List<StageMember> stageMemberList = stageMemberRepository.findByStageId(stageId);

        if (roundList.size() >= stageMemberList.size()) {

            finishStage(stageId);

        } else {

            final Round round = new Round();
            round.id = System.currentTimeMillis();
            round.stageId = stageId;
            round.status = RoundStatus.ON_GOING.name();
            round.currentRoundNumber = roundList.size();
            round.currentTurnNumber = roundList.size();
            round.createDate = Timestamp.valueOf(LocalDateTime.now());
            final Round resultRound = roundRepository.save(round);
            final Long roundId = resultRound.id;

            // send round cards
            List<StageUserCard> stageUserCardList = stageUserCardRepository.findByStageId(stageId);
            Map<String, List<String>> stageUserCardListMap = new HashMap<>();
            for (StageUserCard stageUserCard : stageUserCardList) {
                String userId = stageUserCard.userId;
                String cardId = stageUserCard.cardId;
                if (stageUserCardListMap.get(userId) != null) {
                    List existList = stageUserCardListMap.get(userId);
                    existList.add(cardId);
                    stageUserCardListMap.put(userId, existList);
                } else {
                    List newList = new ArrayList<String>();
                    newList.add(cardId);
                    stageUserCardListMap.put(userId, newList);
                }
            }

            // send situation card to evil
            String evil = stageMemberList.stream()
                    .filter(s -> s.turnNumber == resultRound.currentRoundNumber)
                    .map(s -> s.userId)
                    .findFirst()
                    .get();
            List<Card> situationList = cardRepository.findByType(CardType.SITUATION.name());
            // get situation cards someone already sent
            List<Long> roundIds = roundList.stream().map(r -> r.id).collect(Collectors.toList());
            List<String> roundCardListForSituation =
                    roundCardRepository
                            .findByRoundIdInAndCardIdStartingWith(roundIds, "SITUATION")
                            .stream().map(roundCard -> roundCard.cardId).collect((Collectors.toList()));
            for(int i = 0; i < situationList.size(); i++) {
                Card card = situationList.get(i);
                for(String cardId : roundCardListForSituation) {
                    if(card.id.equals(cardId)) {
                        situationList.remove(i);
                    }
                }
            }
            Collections.shuffle(situationList);
            log.debug("roundCardListForSituation : " + roundCardListForSituation);
            log.debug("situationList : " + situationList);

            List<Card> tempList = new ArrayList<>();
            for (int i = 0; i < situationList.size(); i++ ){
                tempList.add(situationList.get(i));
                if (i % 10 == 9 || i >= (situationList.size() - 1)) {
                    createCard(stageId, roundId, tempList, evil);
                    tempList = new ArrayList<>();
                }
            }
            // send comment for evil
            final String nextYourTurnMessage = messageSource.getMessage(
                    "bot.round.set.round.card.next.your.turn",
                    null,
                    Locale.JAPANESE);
            lineMessage.multicast(Collections.singleton(evil),
                    Collections.singletonList(new TextMessage(nextYourTurnMessage)));


            // send comment and option card to stage member except evil
            for (Map.Entry<String, List<String>> entry : stageUserCardListMap.entrySet()) {
                String userId = entry.getKey();
                List<Card> cardList = cardRepository.findByIdIn(entry.getValue());
                if (!userId.equals(evil)) {
                    createCard(stageId, roundId, cardList, userId);
                }
            }

        }
    }

    private void createCard(String stageId, Long roundId, List<Card> cardList, String userId) {

        List<Bubble> bubbleList = new ArrayList<>();

        for(Card card : cardList) {
            bubbleList.add(__createCardBubble(stageId, roundId, card));
        }

        final Carousel carousel =
                Carousel.builder()
                        .contents(bubbleList)
                        .build();

        final String altText = messageSource.getMessage(
                "bot.round.user.cards.altText",
                null,
                Locale.JAPANESE);

        lineMessage.multicast(
                Collections.singleton(userId),
                Collections.singletonList(new FlexMessage(altText, carousel)));

    }

    private Bubble __createCardBubble(String stageId, Long roundId, Card card){

        final String typeForDisplay = messageSource.getMessage(
                CardType.valueOf(card.type).message,
                null,
                Locale.JAPANESE);

        final Text typeComponent =
                Text.builder()
                        .text(typeForDisplay)
                        .wrap(true)
                        .weight(Text.TextWeight.BOLD)
                        .align(FlexAlign.CENTER)
                        .size(FlexFontSize.XXL)
                        .build();

        final Separator separator =
                Separator.builder()
                        .margin(FlexMarginSize.XXL)
                        .build();

        final Text textComponent =
                Text.builder()
                        .text(card.text)
                        .wrap(true)
                        .align(FlexAlign.CENTER)
                        .size(FlexFontSize.XXL)
                        .margin(FlexMarginSize.XXL)
                        .build();

        final String postbackLabel = messageSource.getMessage(
                "bot.round.user.cards.image.label",
                null,
                Locale.JAPANESE);
        final String postbackText = messageSource.getMessage(
                "bot.round.user.cards.image.text",
                null,
                Locale.JAPANESE);
        final String postbackData = PostBackKeyName.ACTION.keyName + "="
                + PostBackAction.SET_ROUND_CARD.name() + "&"
                + PostBackKeyName.STAGE.keyName + "=" + stageId + "&"
                + PostBackKeyName.ROUND.keyName + "=" + roundId + "&"
                + PostBackKeyName.CARD.keyName + "=" + card.id;

        final Button button =
                Button.builder()
                        .style(Button.ButtonStyle.PRIMARY)
                        .action(new PostbackAction(postbackLabel, postbackData, postbackText))
                        .margin(FlexMarginSize.XXL)
                        .build();

        final Box bodyBox;

        if(!card.title.equals("")) {
            final Text titleComponent =
                    Text.builder()
                            .text(card.title)
                            .wrap(true)
                            .weight(Text.TextWeight.BOLD)
                            .align(FlexAlign.CENTER)
                            .color("#ffc107")
                            .size(FlexFontSize.XXL)
                            .margin(FlexMarginSize.XXL)
                            .build();
            bodyBox = Box.builder()
                    .layout(FlexLayout.VERTICAL)
                    .contents(Arrays.asList(typeComponent, separator, titleComponent, textComponent))
                    .build();
        } else {
            bodyBox = Box.builder()
                    .layout(FlexLayout.VERTICAL)
                    .contents(Arrays.asList(typeComponent, separator, textComponent))
                    .build();
        }

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
        return bubble;

    };

    private List<Round> finishAllRounds(String stageId) {
        List<Round> roundList = roundRepository.findByStageIdOrderByCreateDateDesc(stageId);
        for (Round r : roundList) {
            r.status = RoundStatus.DONE.name();
        }
        return roundRepository.saveAll(roundList);
    }


//////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////
///////////////////////  stage  //////////////////////////
//////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////

    private StageUserCard createStageUserCard(String userId, String stageId, Card card) {
        final StageUserCard stageUserCard = new StageUserCard();
        stageUserCard.id = CommonUtils.getUUID();
        stageUserCard.userId = userId;
        stageUserCard.stageId = stageId;
        stageUserCard.cardId = card.id;
        return stageUserCard;
    }

    private Stage createStageTable() {
        final String stageId = CommonUtils.getUUID();
        final String password = CommonUtils.get6DigitCode();
        final Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        final Stage stage = new Stage();
        stage.id = stageId;
        stage.password = password;
        stage.createDate = now;
        stage.status = StageStatus.PARTICIPANTS_WANTED.name();
        stage.updateDate = now;
        return stageRepository.save(stage);
    }

    private void finishStage(String stageId) {

        finishAllRounds(stageId);
        final Optional<Stage> optionalStage = stageRepository.findById(stageId);
        if (optionalStage.isPresent()) {

            Stage currentStage = optionalStage.get();
            currentStage.status = StageStatus.END_GAME.name();
            currentStage.updateDate = Timestamp.valueOf(LocalDateTime.now());
            stageRepository.save(currentStage);

            List<StageMember> stageMemberList = stageMemberRepository.findByStageId(stageId);
            List<StageMember> terminatedStageMemberList = new ArrayList<>();
            for (StageMember stageMember : stageMemberList) {
                stageMember.status = StageMemberStatus.TERMINATED.name();
                terminatedStageMemberList.add(stageMember);
            }
            stageMemberRepository.saveAll(terminatedStageMemberList);
            stageUserCardRepository.deleteByStageId(stageId);

            Set<String> memberSet = stageMemberList.stream().map(s -> s.userId).collect(Collectors.toSet());
            final String successMessage = messageSource.getMessage(
                    "bot.stage.end.confirm.success",
                    null,
                    Locale.JAPANESE);
            lineMessage.multicast(memberSet,
                    Collections.singletonList(new TextMessage(successMessage)));
        }
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

    private TemplateMessage createParticipantsWantedStageList(List<Stage> stageList) {
        List<CarouselColumn> carouselColumnList = new ArrayList<>();
        for(Stage stage : stageList){

            final String carouselTitle = messageSource.getMessage(
                    "bot.stage.join.request.title",
                    new Object[]{stage.id},
                    Locale.JAPANESE);
            final String carouselText = messageSource.getMessage(
                    "bot.stage.join.request.text",
                    null,
                    Locale.JAPANESE);
            final String postbackLabel = messageSource.getMessage(
                    "bot.stage.join.request.button.label",
                    null,
                    Locale.JAPANESE);
            final String postbackText = messageSource.getMessage(
                    "bot.stage.join.request.button.text",
                    new Object[]{stage.id},
                    Locale.JAPANESE);
            final String postbackData = PostBackKeyName.ACTION.keyName + "="
                    + PostBackAction.REQUEST_TO_JOIN_STAGE.name() + "&"
                    + PostBackKeyName.STAGE.keyName + "=" + stage.id;

            final CarouselColumn carouselColumn = new CarouselColumn(
                    null, carouselTitle, carouselText, Arrays.asList(
                    new PostbackAction(postbackLabel, postbackData, postbackText)));

            carouselColumnList.add(carouselColumn);
        }

        final String altText = messageSource.getMessage(
                "bot.stage.join.request.altText",
                null,
                Locale.JAPANESE);

        return new TemplateMessage(altText, new CarouselTemplate(carouselColumnList));
    }


//////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////
////////////////  stage member  //////////////////////////
//////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////

    private void addMemberAndSendMessageToMember(String userId, Stage stage) {
        addMember(userId, stage.id);
        sendMessageForJoiningMember_doNotCallDirectly(userId, stage);
    }

    private void addMember(String userId, String stageId) {
        final UserProfileResponse userProfileResponse = lineMessage.getProfile(userId);
        addMember_doNotCallDirectly(
                stageId,
                userId,
                userProfileResponse.getDisplayName(),
                userProfileResponse.getPictureUrl(),
                StageMemberStatus.JOINING.name());
    }

    private void addTempMember(String userId, String stageId) {
        final UserProfileResponse userProfileResponse = lineMessage.getProfile(userId);
        addMember_doNotCallDirectly(
                stageId,
                userId,
                userProfileResponse.getDisplayName(),
                userProfileResponse.getPictureUrl(),
                StageMemberStatus.APPLY_TO_JOIN.name());
    }

    private void addMemberAndSendMessageToMemberFromWeb(OAuth2Authentication oAuth2Authentication, Stage stage) {
        Map<String, Object> properties = (Map<String, Object>) oAuth2Authentication.getUserAuthentication().getDetails();
        final String userId = (String) properties.get("userId");
        final String displayName = (String) properties.get("displayName");
        final String pictureUrl = (String) properties.get("pictureUrl");
        addMember_doNotCallDirectly(
                stage.id,
                userId,
                displayName,
                pictureUrl,
                StageMemberStatus.JOINING.name());

        sendMessageForJoiningMember_doNotCallDirectly(userId, stage);
    }

    private void addMember_doNotCallDirectly(
            final String stageId,
            final String userId,
            final String displayName,
            final String pictureUrl,
            final String status) {

        final Optional<StageMember> optionalStageMember = getStageMemberForEachUser(userId);
        if (optionalStageMember.isPresent()) {
            List<String> statusList = Arrays.asList(
                    StageMemberStatus.APPLY_TO_JOIN.name(),
                    StageMemberStatus.JOINING.name());
            stageMemberRepository.deleteByUserIdAndStatusIn(userId, statusList);
        }
        final StageMember stageMember = new StageMember();
        stageMember.id = CommonUtils.getUUID();
        stageMember.stageId = stageId;
        stageMember.userId = userId;
        stageMember.userName = displayName;
        stageMember.pictureUrl = pictureUrl;
        stageMember.status = status;
        stageMember.createDate = Timestamp.valueOf(LocalDateTime.now());
        stageMemberRepository.save(stageMember);
        publishToStompClient(stageId);
    }

    private void sendMessageForJoiningMember_doNotCallDirectly(String userId, Stage stage) {
        final String url = CommonUtils.createStageUrl(stage.id);
        final String correctPassword = messageSource.getMessage(
                "bot.stage.input.password.correct.password",
                new Object[]{stage.id, url},
                Locale.JAPANESE);
            log.debug("addMemberAndSendMessageToMember start 心理的安全性ゲームBot");
        try {
            lineMessage.multicast(Collections.singleton(userId),
                    Collections.singletonList(new TextMessage(correctPassword)));
        } catch (RuntimeException ex) {
            log.debug("addMemberAndSendMessageToMember RuntimeException 心理的安全性ゲームBot");
            log.debug(ex.getMessage());
        }
    }

    private boolean userAlreadyJoinedStage(String userId) {
        List<StageMember> userStageMemberList =
                stageMemberRepository.findByUserIdAndStatus(userId, StageMemberStatus.JOINING.name());
        Optional<StageMember> optionalStageMember = Optional.empty();
        if (userStageMemberList.size() > 0 ) {
            optionalStageMember = Optional.of(userStageMemberList.get(0));
        }
        if (optionalStageMember.isPresent()) {
            final String alreadyJoined = messageSource.getMessage(
                    "bot.stage.already.joined",
                    new Object[]{optionalStageMember.get().stageId},
                    Locale.JAPANESE);
            lineMessage.multicast(Collections.singleton(userId), Collections.singletonList(new TextMessage(alreadyJoined)));
        }
        return optionalStageMember.isPresent();
    }

    private Optional<StageMember> getApplyingStage(String userId) {
        List<StageMember> userStageMemberList =
                stageMemberRepository.findByUserIdAndStatus(userId, StageMemberStatus.APPLY_TO_JOIN.name());
        Optional<StageMember> optionalStageMember = Optional.empty();
        if (userStageMemberList.size() > 0 ) {
            optionalStageMember = Optional.of(userStageMemberList.get(0));
        }
        if (! optionalStageMember.isPresent()) {
            final String applyingStageNotFound = messageSource.getMessage(
                    "bot.stage.input.password.applying.stage.not.found",
                    null,
                    Locale.JAPANESE);
            lineMessage.multicast(Collections.singleton(userId), Collections.singletonList(new TextMessage(applyingStageNotFound)));
        }
        return optionalStageMember;
    }

    private void publishToStompClient(String stageId){
        String subscriptionUrl = WebSocketConfig.DESTINATION_STAGE_PREFIX + "/" + stageId;
        log.debug("subscriptionUrl : " + subscriptionUrl);
        simpMessagingTemplate.convertAndSend(
                subscriptionUrl,
                "hello");
    }
}
