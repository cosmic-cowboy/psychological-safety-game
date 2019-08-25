package com.slgerkamp.psychological.safety.game.domain.game.service;

import com.linecorp.bot.model.action.PostbackAction;
import com.linecorp.bot.model.message.FlexMessage;
import com.linecorp.bot.model.message.Message;
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
import com.linecorp.bot.model.message.quickreply.QuickReply;
import com.linecorp.bot.model.message.quickreply.QuickReplyItem;
import com.slgerkamp.psychological.safety.game.application.model.RoundCardForView;
import com.slgerkamp.psychological.safety.game.domain.game.*;
import com.slgerkamp.psychological.safety.game.infra.message.LineMessage;
import com.slgerkamp.psychological.safety.game.infra.model.*;
import com.slgerkamp.psychological.safety.game.infra.utils.CommonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class RoundService {

    @Autowired
    private StageMemberRepository stageMemberRepository;
    @Autowired
    private RoundRepository roundRepository;
    @Autowired
    private RoundCardRepository roundCardRepository;
    @Autowired
    private RoundRetrospectiveRepository roundRetrospectiveRepository;
    @Autowired
    private StageUserCardRepository stageUserCardRepository;
    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private LineMessage lineMessage;
    @Autowired
    private MessageSource messageSource;
    @Autowired
    private NotificationService notificationService;

    public Map<Long, List<RoundCardForView>> getRoundCards(String stageId) {
        List<Round> roundList = roundRepository.findByStageIdOrderByCreateDateDesc(stageId);
        Map<Long, List<RoundCardForView>> roundCardMap = new TreeMap<>();

        if (roundList.size() > 0) {
            final List<Long> roundIdList = roundList.stream().map(r -> r.id).collect(Collectors.toList());
            final List<RoundCard> roundCardList = roundCardRepository.findByRoundIdInOrderByCreateDateDesc(roundIdList);
            final List<RoundRetrospective> roundRetrospectiveList =
                    roundRetrospectiveRepository.findByRoundIdInOrderByCreateDateDesc(roundIdList);
            final List<Card> cardList = cardRepository.findAll();

            List<RoundCardForView> roundCardForViewList = new ArrayList<>();

            for (RoundRetrospective roundRetrospective : roundRetrospectiveList) {
                Card card = cardList.stream().filter(s -> s.id.endsWith(roundRetrospective.cardId)).findFirst().get();

                RoundCardForView roundCardForView = new RoundCardForView();
                roundCardForView.roundId = roundRetrospective.roundId;
                roundCardForView.turnNumber = 0;
                roundCardForView.userId = roundRetrospective.userId;
                roundCardForView.cardId = roundRetrospective.cardId;
                roundCardForView.type = card.type;
                roundCardForView.text = messageSource.getMessage(
                        "card.future.team.condition." +  roundRetrospective.answer,
                        null,
                        Locale.JAPANESE);
                roundCardForViewList.add(roundCardForView);

                RoundCardForView roundCardForViewQuestion = new RoundCardForView();
                roundCardForViewQuestion.roundId = roundRetrospective.roundId;
                roundCardForViewQuestion.turnNumber = 0;
                roundCardForViewQuestion.userId = "defaultIcon";
                roundCardForViewQuestion.cardId = roundRetrospective.cardId;
                roundCardForViewQuestion.type = card.type;
                roundCardForViewQuestion.text = card.text;
                roundCardForViewList.add(roundCardForViewQuestion);

            }

            for (RoundCard roundCard : roundCardList) {
                Card card = cardList.stream().filter(s -> s.id.endsWith(roundCard.cardId)).findFirst().get();
                RoundCardForView roundCardForView = new RoundCardForView();
                roundCardForView.roundId = roundCard.roundId;
                roundCardForView.turnNumber = roundCard.turnNumber;
                roundCardForView.userId = roundCard.userId;
                roundCardForView.cardId = roundCard.cardId;
                roundCardForView.type = card.type;
                roundCardForView.text = card.text;
                roundCardForViewList.add(roundCardForView);
            }

            Map<Long, List<RoundCardForView>> hashRoundCardMap =
                    roundCardForViewList.stream().collect(Collectors.groupingBy(r -> r.roundId));
            roundCardMap = new TreeMap<>(hashRoundCardMap).descendingMap();

        }
        return roundCardMap;
    }

    public Map<String, Map<String, List<String>>> getRoundRetrospective(String stageId) {
        List<Round> roundList = roundRepository.findByStageIdOrderByCreateDateDesc(stageId);
        Map<String, Map<String, List<String>>> roundRetrospectiveMap = new TreeMap<>();
        if (roundList.size() > 0) {
            final List<Long> roundIdList = roundList.stream().map(r -> r.id).collect(Collectors.toList());
            List<RoundRetrospective> roundRetrospectiveList =
                    roundRetrospectiveRepository.findByRoundIdInOrderByCreateDateDesc(roundIdList);
            List<Card> themeList = cardRepository.findByTypeOrderByCreateDate(CardType.THEME.name());

            for(Card card : themeList) {
                Map<String, List<String>> answerMap = new TreeMap<>();
                for(int i = 1; i < 6; i++) {
                    String answer = String.valueOf(i);
                    List<String> userIdList =
                            roundRetrospectiveList
                                    .stream()
                                    .filter(s -> s.cardId.equals(card.id) && s.answer.equals(answer))
                                    .map(s -> s.userId)
                                    .collect(Collectors.toList());
                    answerMap.put(answer, userIdList);
                }
                roundRetrospectiveMap.put(card.text, answerMap);
            }
        }
        return roundRetrospectiveMap;
    }
///////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////
///////////////////  default method  //////////////////////
///////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////

    boolean storeRoundCardOrThemeCardAndSendMessage(String userId, Long roundId, String cardId, Optional<String> optinalThemeAnswer) {
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
                List<StageMember> stageMemberList = stageMemberRepository.findByStageId(round.stageId);

                // set round card
                if (!optinalThemeAnswer.isPresent()) {
                    storeRoundCardAndSendMessage(userId, roundId, card, round, turnNumber);
                    prepareForNextMember(card, round, stageMemberList);
                    notificationService.publishToStompClient(round.stageId);

                // set theme card
                } else {
                    String themeAnswer = optinalThemeAnswer.get();
                    storeThemeCard(userId, card, round, themeAnswer);
                    notificationService.publishToStompClient(round.stageId);
                    return createThemeMessageAndSendMessage(round, userId);
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
        return true;
    }

    void createRoundSettings(List<StageMember> stageMemberList) {
        // set cards for this stage
        List<Card> commentsCards = cardRepository.findByTypeOrderByCreateDate(CardType.COMMENT.name());
        List<Card> optionCards = cardRepository.findByTypeOrderByCreateDate(CardType.OPTION.name());

        Collections.shuffle(stageMemberList);
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
    }

    boolean createNewRound(String stageId){
        // finish All Rounds just in case and get All rounds
        List<Round> roundList = finishAllRounds(stageId);
        // get stage member list
        List<StageMember> stageMemberList = stageMemberRepository.findByStageId(stageId);

        if (roundList.size() >= stageMemberList.size()) {

            // finishStage(stageId);
            return false;

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
            List<Card> situationList = cardRepository.findByTypeOrderByCreateDate(CardType.SITUATION.name());
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
            // send card and comment for evil
            createCardAndSendMessage(stageId, roundId, Collections.singletonList(situationList.get(0)), evil);
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
                    createCardAndSendMessage(stageId, roundId, cardList, userId);
                }
            }

        }
        return true;
    }

    List<Round> finishAllRounds(String stageId) {
        List<Round> roundList = roundRepository.findByStageIdOrderByCreateDateDesc(stageId);
        for (Round r : roundList) {
            r.status = RoundStatus.DONE.name();
        }
        return roundRepository.saveAll(roundList);
    }

    void deleteByStageId(String stageId) {
        stageUserCardRepository.deleteByStageId(stageId);
    }

///////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////
///////////////////  private method  //////////////////////
///////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////

    private StageUserCard createStageUserCard(String userId, String stageId, Card card) {
        final StageUserCard stageUserCard = new StageUserCard();
        stageUserCard.id = CommonUtils.getUUID();
        stageUserCard.userId = userId;
        stageUserCard.stageId = stageId;
        stageUserCard.cardId = card.id;
        return stageUserCard;
    }

    private void createCardAndSendMessage(String stageId, Long roundId, List<Card> cardList, String userId) {
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

    private void storeRoundCardAndSendMessage(String userId, Long roundId, Card card, Round round, Integer turnNumber) {
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
    }

    private void prepareForNextMember(Card card, Round round, List<StageMember> stageMemberList) {
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

        if (nextCurrentTurnNumber != round.currentRoundNumber) {
            final String nextYourTurnMessage = messageSource.getMessage(
                    "bot.round.set.round.card.next.your.turn",
                    null,
                    Locale.JAPANESE);
            lineMessage.multicast(Collections.singleton(nextUserId),
                    Collections.singletonList(new TextMessage(nextYourTurnMessage)));
        } else {
            createThemeMessageAndSendMessage(round, nextUserId);
        }
    }

    private void storeThemeCard(String userId, Card card, Round round, String themeAnswer) {
        RoundRetrospective roundRetrospective = new RoundRetrospective();
        roundRetrospective.id =  CommonUtils.getUUID();
        roundRetrospective.roundId = round.id;
        roundRetrospective.userId = userId;
        roundRetrospective.cardId = card.id;
        roundRetrospective.answer = themeAnswer;
        roundRetrospective.createDate = Timestamp.valueOf(LocalDateTime.now());
        roundRetrospectiveRepository.save(roundRetrospective);
    }

    private boolean createThemeMessageAndSendMessage(Round round, String userId) {
        List<Card> themeList = cardRepository.findByTypeOrderByCreateDate(CardType.THEME.name());
        List<RoundRetrospective> roundRetrospectives =
                roundRetrospectiveRepository.findByRoundIdOrderByCreateDateDesc(round.id);
        int currentRetrospectiveSize = roundRetrospectives.size();

        if (themeList.size() <= currentRetrospectiveSize) {
            return createNewRound(round.stageId);
        }

        List<Message> messageList = new ArrayList<>();
        if (currentRetrospectiveSize == 0) {
            final String nextYourTurnMessage = messageSource.getMessage(
                    "bot.round.set.round.card.for.theme.next.your.turn",
                    null,
                    Locale.JAPANESE);
            messageList.add(new TextMessage(nextYourTurnMessage));
        }
        Card themeCard = themeList.get(currentRetrospectiveSize);
        final String question = messageSource.getMessage(
                "bot.round.set.round.card.for.theme.question",
                new Object[]{themeCard.text},
                Locale.JAPANESE);
        final QuickReply quickReply = __createThemeQuickReply(round.stageId, round.id, themeCard);
        messageList.add(TextMessage.builder().text(question).quickReply(quickReply).build());

        lineMessage.multicast(Collections.singleton(userId),messageList);
        return true;
    }
    
    private QuickReply __createThemeQuickReply(String stageId, Long roundId, Card card) {
        final String condition1 = messageSource.getMessage(
                "card.future.team.condition.1",
                null,
                Locale.JAPANESE);
        final String condition2 = messageSource.getMessage(
                "card.future.team.condition.2",
                null,
                Locale.JAPANESE);
        final String condition3 = messageSource.getMessage(
                "card.future.team.condition.3",
                null,
                Locale.JAPANESE);
        final String condition4 = messageSource.getMessage(
                "card.future.team.condition.4",
                null,
                Locale.JAPANESE);
        final String condition5 = messageSource.getMessage(
                "card.future.team.condition.5",
                null,
                Locale.JAPANESE);
        final String postbackData = PostBackKeyName.ACTION.keyName + "="
                + PostBackAction.SET_THEME_CARD.name() + "&"
                + PostBackKeyName.STAGE.keyName + "=" + stageId + "&"
                + PostBackKeyName.ROUND.keyName + "=" + roundId + "&"
                + PostBackKeyName.CARD.keyName + "=" + card.id + "&"
                + PostBackKeyName.THEME_ANSWER.keyName + "=";


        final List<QuickReplyItem> items = Arrays.<QuickReplyItem>asList(
                QuickReplyItem.builder()
                        .action(PostbackAction.builder()
                                .label(condition1)
                                .text(condition1)
                                .data(postbackData +"1")
                                .build())
                        .build(),
                QuickReplyItem.builder()
                        .action(PostbackAction.builder()
                                .label(condition2)
                                .text(condition2)
                                .data(postbackData +"2")
                                .build())
                        .build(),
                QuickReplyItem.builder()
                        .action(PostbackAction.builder()
                                .label(condition3)
                                .text(condition3)
                                .data(postbackData +"3")
                                .build())
                        .build(),
                QuickReplyItem.builder()
                        .action(PostbackAction.builder()
                                .label(condition4)
                                .text(condition4)
                                .data(postbackData +"4")
                                .build())
                        .build(),
                QuickReplyItem.builder()
                        .action(PostbackAction.builder()
                                .label(condition5)
                                .text(condition5)
                                .data(postbackData +"5")
                                .build())
                        .build()
        );
        final QuickReply quickReply = QuickReply.items(items);
        return quickReply;
    }
}
