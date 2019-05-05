package com.slgerkamp.psychological.safety.game.domain.game.service;

import com.linecorp.bot.model.action.PostbackAction;
import com.linecorp.bot.model.message.TemplateMessage;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.message.template.*;
import com.linecorp.bot.model.profile.UserProfileResponse;
import com.slgerkamp.psychological.safety.game.application.controller.GameController;
import com.slgerkamp.psychological.safety.game.domain.game.*;
import com.slgerkamp.psychological.safety.game.infra.message.LineMessage;
import com.slgerkamp.psychological.safety.game.infra.model.*;
import com.slgerkamp.psychological.safety.game.infra.utils.CommonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

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
    private LineMessage lineMessage;

    @Autowired
    private MessageSource messageSource;

    public void createStage(String userId) {
        // check whether sender already join a stage or not
        if (!userAlreadyJoinedStage(userId)) {

            final Stage result_stage = createStage();

            addMember(userId, result_stage.id);

            // send password to creator
            String msg = messageSource.getMessage(
                    "bot.stage.create.success",
                    new Object[]{result_stage.id, result_stage.password},
                    Locale.JAPANESE);
            lineMessage.multicast(Collections.singleton(userId), Collections.singletonList(new TextMessage(msg)));
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

    public void requestToJoinStage(String userId, String stageId) {
        // check whether sender already join a stage or not
        if (!userAlreadyJoinedStage(userId)) {

            // fetch stagesParticipantsWanted
            Optional<Stage> optionalStage = getParticipantsWantedStage(stageId);

            if (optionalStage.isPresent()) {
                addTempMember(userId, stageId);
                final String alreadyJoined = messageSource.getMessage(
                        "bot.stage.join.confirm",
                        new Object[]{stageId},
                        Locale.JAPANESE);
                lineMessage.multicast(Collections.singleton(userId),
                        Collections.singletonList(new TextMessage(alreadyJoined)));
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
                            addMember(userId, stage.id);
                            final String wrongPassword = messageSource.getMessage(
                                    "bot.stage.input.password.correct.password",
                                    new Object[]{stage.id},
                                    Locale.JAPANESE);
                            lineMessage.multicast(Collections.singleton(userId),
                                    Collections.singletonList(new TextMessage(wrongPassword)));
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

            ButtonsTemplate buttonsTemplate = new ButtonsTemplate(
                    null, buttonTitle, buttonText, Arrays.asList(
                    new PostbackAction(postbackLabel, postbackData, postbackText)));

            final String altText = messageSource.getMessage(
                    "bot.stage.start.request.altText",
                    null,
                    Locale.JAPANESE);
            lineMessage.multicast(Collections.singleton(userId),
                    Collections.singletonList(new TemplateMessage(altText, buttonsTemplate)));
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
            Stage currentStage = optionalStage.get();
            currentStage.status = StageStatus.START_GAME.name();
            stageRepository.save(currentStage);

            // set turn_number for this stage
            // set cards for this stage
            List<StageMember> stageMemberList = stageMemberRepository.findByStageId(stageId);
            Collections.shuffle(stageMemberList);

            List<String> commentsCards = Cards.getTypeList("comment");
            List<String> optionCards = Cards.getTypeList("option");
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
        List<StageMember> userStageMemberList = stageMemberRepository.findByUserId(userId);

        if (optionalRound.isPresent() && userStageMemberList.size() > 0) {
            StageMember userStageMember = userStageMemberList.get(0);
            Round round = optionalRound.get();

            Integer turnNumber = userStageMember.turnNumber;

            if (round.currentTurnNumber == turnNumber) {

                RoundCard roundCard = new RoundCard();
                roundCard.id = CommonUtils.getUUID();
                roundCard.roundId = roundId;
                roundCard.userId = userId;
                roundCard.cardId = cardId;
                roundCard.turnNumber = turnNumber;
                roundCard.createDate = Timestamp.valueOf(LocalDateTime.now());
                roundCardRepository.save(roundCard);
                stageUserCardRepository.deleteByStageIdAndUserIdAndCardId(round.stageId, userId, cardId);

                final String successMessage = messageSource.getMessage(
                        "bot.round.set.round.card.success",
                        null,
                        Locale.JAPANESE);
                lineMessage.multicast(Collections.singleton(userId),
                        Collections.singletonList(new TextMessage(successMessage)));

                // Go to next turn
                List<StageMember> stageMemberList = stageMemberRepository.findByStageId(round.stageId);

                Integer nextCurrentTurnNumber;
                if(!cardId.startsWith("THEME") && !cardId.startsWith("OPTION")) {

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

                    if (cardId.startsWith("THEME")) {
                        createNewRound(round.stageId);
                    } else {
                        List<String> themeList = Cards.getTypeList("theme");
                        createCard(round.stageId, round.id, themeList, nextUserId);
                    }
                }

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

    public Optional<StageMember> getStageMember(String userId) {
        Optional<StageMember> optionalStageMember = Optional.empty();
        List<StageMember> stageMemberList = stageMemberRepository.findByUserId(userId);
        if (stageMemberList.size() > 0) {
            optionalStageMember = Optional.of(stageMemberList.get(0));
        }
        return optionalStageMember;
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

            ButtonsTemplate buttonsTemplate = new ButtonsTemplate(
                    null, buttonTitle, buttonText, Arrays.asList(
                    new PostbackAction(postbackLabel, postbackData, postbackText)));

            final String altText = messageSource.getMessage(
                    "bot.stage.end.request.altText",
                    null,
                    Locale.JAPANESE);
            lineMessage.multicast(Collections.singleton(userId),
                    Collections.singletonList(new TemplateMessage(altText, buttonsTemplate)));
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


    public List<RoundCard> getRoundCards(String stageId) {
        List<Round> roundList = roundRepository.findByStageIdOrderByCreateDateDesc(stageId);
        List<RoundCard> result = new ArrayList<>();
        if (roundList.size() > 0) {
            List<Long> roundIdList = roundList.stream().map(r -> r.id).collect(Collectors.toList());
            List<RoundCard> roundCardList = roundCardRepository.findByRoundIdIn(roundIdList);

        }
        return result;
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
            List<String> situationList = Cards.getTypeList("situation");
            // get situation cards someone already sent
            List<Long> roundIds = roundList.stream().map(r -> r.id).collect(Collectors.toList());
            List<String> roundCardListForSituation =
                    roundCardRepository
                            .findByRoundIdInAndCardIdStartingWith(roundIds, "SITUATION")
                            .stream().map(roundCard -> roundCard.cardId).collect((Collectors.toList()));
            situationList.removeAll(roundCardListForSituation);
            log.debug("roundCardListForSituation : " + roundCardListForSituation);
            log.debug("situationList : " + situationList);

            List<String> tempList = new ArrayList<>();
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
                if (!userId.equals(evil)) {
                    createCard(stageId, roundId, entry.getValue(), userId);
                }
            }

        }
    }

    private void createCard(String stageId, Long roundId, List<String> cardList, String userId) {
        final String postbackLabel = messageSource.getMessage(
                "bot.round.user.cards.image.label",
                null,
                Locale.JAPANESE);
        final String postbackText = messageSource.getMessage(
                "bot.round.user.cards.image.text",
                null,
                Locale.JAPANESE);
        final String altText = messageSource.getMessage(
                "bot.round.user.cards.altText",
                null,
                Locale.JAPANESE);

        List<ImageCarouselColumn> imageCarouselColumns = new ArrayList<>();
        for(String cardId : cardList) {
            final String postbackData = PostBackKeyName.ACTION.keyName + "="
                    + PostBackAction.SET_ROUND_CARD.name() + "&"
                    + PostBackKeyName.STAGE.keyName + "=" + stageId + "&"
                    + PostBackKeyName.ROUND.keyName + "=" + roundId + "&"
                    + PostBackKeyName.CARD.keyName + "=" + cardId;
            final ImageCarouselColumn imageCarouselColumn = new ImageCarouselColumn(
                    Cards.getFileName(cardId),
                    new PostbackAction(postbackLabel, postbackData, postbackText));
            imageCarouselColumns.add(imageCarouselColumn);
        }

        TemplateMessage templateMessage =
                new TemplateMessage(altText, new ImageCarouselTemplate(imageCarouselColumns));
        lineMessage.multicast(
                Collections.singleton(userId), Collections.singletonList(templateMessage));
    }

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

    private StageUserCard createStageUserCard(String userId, String stageId, String cardId) {
        final StageUserCard stageUserCard = new StageUserCard();
        stageUserCard.id = CommonUtils.getUUID();
        stageUserCard.userId = userId;
        stageUserCard.stageId = stageId;
        stageUserCard.cardId = cardId;
        return stageUserCard;
    }

    private Stage createStage() {

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
            List<StageMember> stageMemberList = stageMemberRepository.findByStageId(stageId);
            Set<String> memberSet = stageMemberList.stream().map(s -> s.userId).collect(Collectors.toSet());

            Stage currentStage = optionalStage.get();

            currentStage.status = StageStatus.END_GAME.name();
            stageRepository.save(currentStage);
            stageMemberRepository.deleteByStageId(stageId);
            stageUserCardRepository.deleteByStageId(stageId);

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

    private void addTempMember(String userId, String stageId) {
        addMember_doNotCallDirectly(userId, stageId, StageMemberStatus.APPLY_TO_JOIN.name());
    }

    private void addMember(String userId, String stageId) {
        addMember_doNotCallDirectly(userId, stageId, StageMemberStatus.JOINING.name());
    }

    private void addMember_doNotCallDirectly(final String userId, final String stageId, final String status) {
        final Optional<StageMember> optionalStageMember = getStageMember(userId);
        if (optionalStageMember.isPresent()) {
            stageMemberRepository.deleteByUserId(userId);
        }

        final UserProfileResponse userProfileResponse = lineMessage.getProfile(userId);
        final StageMember stageMember = new StageMember();
        stageMember.id = CommonUtils.getUUID();
        stageMember.stageId = stageId;
        stageMember.userId = userProfileResponse.getUserId();
        stageMember.userName = userProfileResponse.getDisplayName();
        stageMember.status = status;
        stageMemberRepository.save(stageMember);
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

}
