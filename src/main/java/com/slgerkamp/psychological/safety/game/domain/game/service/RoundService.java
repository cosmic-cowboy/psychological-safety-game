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
import com.slgerkamp.psychological.safety.game.application.controller.GameController;
import com.slgerkamp.psychological.safety.game.application.model.RoundCardForView;
import com.slgerkamp.psychological.safety.game.domain.game.*;
import com.slgerkamp.psychological.safety.game.infra.message.LineMessage;
import com.slgerkamp.psychological.safety.game.infra.message.ReplyToken;
import com.slgerkamp.psychological.safety.game.infra.model.*;
import com.slgerkamp.psychological.safety.game.infra.utils.CommonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class RoundService {

    private static final Logger log = LoggerFactory.getLogger(GameController.class);

    @Autowired
    private StageMemberRepository stageMemberRepository;
    @Autowired
    private RoundRepository roundRepository;
    @Autowired
    private RoundCardRepository roundCardRepository;
    @Autowired
    private RoundRetrospectiveRepository roundRetrospectiveRepository;
    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private LineMessage lineMessage;
    @Autowired
    private MessageSource messageSource;

    public Map<Long, List<RoundCardForView>> getRoundCards(String stageId) {
        List<Round> roundList = roundRepository.findByStageIdOrderByCreateDateDesc(stageId);
        Map<Long, List<RoundCardForView>> roundCardMap = new TreeMap<>();

        if (roundList.size() > 0) {
            final List<Long> roundIdList = roundList.stream().map(r -> r.id).collect(Collectors.toList());
            final List<RoundCard> roundCardList = roundCardRepository.findByRoundIdInOrderByCreateDateAsc(roundIdList);
            final List<RoundRetrospective> roundRetrospectiveList =
                    roundRetrospectiveRepository.findByRoundIdInOrderByCreateDateAsc(roundIdList);
            final List<Card> cardList = cardRepository.findAll();

            List<RoundCardForView> roundCardForViewList = new ArrayList<>();

            for (RoundCard roundCard : roundCardList) {
                Card card = cardList.stream().filter(s -> s.id.endsWith(roundCard.cardId)).findFirst().get();
                RoundCardForView roundCardForView = new RoundCardForView();
                roundCardForView.roundId = roundCard.roundId;
                roundCardForView.userId = roundCard.userId;
                roundCardForView.cardId = roundCard.cardId;
                roundCardForView.type = card.type;
                roundCardForView.text = card.text;
                roundCardForView.createDate = String.valueOf(roundCard.createDate.getTime());;
                roundCardForViewList.add(roundCardForView);
            }

            for (RoundRetrospective roundRetrospective : roundRetrospectiveList) {
                Card card = cardList.stream().filter(s -> s.id.endsWith(roundRetrospective.cardId)).findFirst().get();

                RoundCardForView roundCardForViewQuestion = new RoundCardForView();
                roundCardForViewQuestion.roundId = roundRetrospective.roundId;
                roundCardForViewQuestion.userId = "defaultIcon";
                roundCardForViewQuestion.cardId = roundRetrospective.cardId;
                roundCardForViewQuestion.type = card.type;
                roundCardForViewQuestion.text = card.text;
                roundCardForViewQuestion.createDate = String.valueOf(roundRetrospective.createDate.getTime());
                roundCardForViewList.add(roundCardForViewQuestion);

                RoundCardForView roundCardForView = new RoundCardForView();
                roundCardForView.roundId = roundRetrospective.roundId;
                roundCardForView.userId = roundRetrospective.userId;
                roundCardForView.cardId = roundRetrospective.cardId;
                roundCardForView.type = card.type;
                roundCardForView.text = messageSource.getMessage(
                        "card.future.team.condition." +  roundRetrospective.answer,
                        null,
                        Locale.JAPANESE);
                roundCardForView.createDate = String.valueOf(roundRetrospective.createDate.getTime());
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
        Map<String, Map<String, List<String>>> roundRetrospectiveMap = new LinkedHashMap<>();
        if (roundList.size() > 0) {
            final List<Long> roundIdList = roundList.stream().map(r -> r.id).collect(Collectors.toList());
            List<RoundRetrospective> roundRetrospectiveList =
                    roundRetrospectiveRepository.findByRoundIdInOrderByCreateDateAsc(roundIdList);
            List<Card> themeList = cardRepository.findByTypeOrderByCreateDate(CardType.THEME.name());

            for(Card card : themeList) {
                Map<String, List<String>> answerMap = new LinkedHashMap<>();
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

    boolean storeRoundCardOrThemeCardAndSendMessage(ReplyToken replyToken, String userId, Long roundId, String cardId, Optional<String> optionalThemeAnswer) {

        Optional<Round> optionalRound = roundRepository.findById(roundId);
        Card card = cardRepository.findById(cardId).get();
        List<StageMember> userStageMemberListWaitingForJoining =
                stageMemberRepository.findByUserIdAndStatus(userId, StageMemberStatus.JOINING.name());

        // check if stage is active, check if this post is normal
        if (optionalRound.isPresent() && userStageMemberListWaitingForJoining.size() > 0) {
            Round round = optionalRound.get();
            List<StageMember> stageMemberList = stageMemberRepository.findByStageId(round.stageId);
            List<RoundCard> roundCardList = roundCardRepository.findByRoundIdInOrderByCreateDateAsc(Collections.singletonList(roundId));

            // This is for situation and comment post
            if (!optionalThemeAnswer.isPresent()) {
                storeRoundCardAndSendMessage(replyToken, userId, roundId, card);
                // This is for situation post
                if(card.type.equals(CardType.SITUATION.name())){
                    // send comment and option card to stage members except evil
                    HashSet<String> stageMemberExceptEvil = getStageMemberExceptEvil(stageMemberList, userId);
                    if (stageMemberExceptEvil.size() > 0) {
                        createAndSendCommentMessage(round, stageMemberExceptEvil);
                    }
                }
                // This is for the last comment post
                if (roundCardList.size() >= stageMemberList.size() - 1){
                    String evilUser;
                    if (roundCardList.size() == 0) {
                        evilUser = userId;
                    } else {
                        evilUser = roundCardList.get(0).userId;
                    }
                    return createAndSendThemeMessage(Optional.empty(), round, evilUser);
                }
            // This is for theme post
            } else {
                String themeAnswer = optionalThemeAnswer.get();
                storeThemeCard(userId, card, round, themeAnswer);
                return createAndSendThemeMessage(Optional.of(replyToken), round, userId);
            }
        // This post is exception
        } else {
            final String errorMessage = messageSource.getMessage(
                    "bot.round.set.round.card.error",
                    null,
                    Locale.JAPANESE);
            lineMessage.reply(replyToken, Collections.singletonList(new TextMessage(errorMessage)));
        }
        return true;
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
            List<Long> roundIds = roundList.stream().map(r -> r.id).collect(Collectors.toList());
            // get situation card
            List<Card> situationList = cardRepository.findByTypeOrderByCreateDate(CardType.SITUATION.name());
            List<RoundCard> roundCardList = roundCardRepository.findByRoundIdInOrderByCreateDateAsc(roundIds);
            List<RoundCard> roundCardListForSituation = new ArrayList<>();
            for (RoundCard r : roundCardList) {
                for (Card c : situationList) {
                    if (c.id.equals(r.cardId)) {
                        roundCardListForSituation.add(r);
                    }
                }
            }
            List<String> situationCardListAlreadySentOnThisRound =
                    roundCardListForSituation.stream().map(roundCard -> roundCard.cardId).collect((Collectors.toList()));
            List<String> userIdListAlreadySentSituationCardOnThisRound =
                    roundCardListForSituation.stream().map(roundCard -> roundCard.userId).collect((Collectors.toList()));

            log.debug("roundCardListForSituation : " + roundCardListForSituation.size());
            log.debug("situationCardListAlreadySentOnThisRound : " + situationCardListAlreadySentOnThisRound.size());
            log.debug("userIdListAlreadySentSituationCardOnThisRound : " + userIdListAlreadySentSituationCardOnThisRound.size());


            for(int i = 0; i < situationList.size(); i++) {
                Card card = situationList.get(i);
                for(String cardId : situationCardListAlreadySentOnThisRound) {
                    if(card.id.equals(cardId)) {
                        situationList.remove(i);
                    }
                }
            }
            Collections.shuffle(situationList);
            Card situationCard = situationList.get(0);

            // set a round info
            final Round round = new Round();
            round.id = System.currentTimeMillis();
            round.stageId = stageId;
            round.status = RoundStatus.ON_GOING.name();
            round.situationCardId = situationCard.id;
            round.currentRoundNumber = roundList.size();
            round.createDate = Timestamp.valueOf(LocalDateTime.now());
            final Round resultRound = roundRepository.save(round);
            final Long roundId = resultRound.id;

            // send a situation card to evil
            final StageMember evil;
            if (userIdListAlreadySentSituationCardOnThisRound.isEmpty()) {
                evil = stageMemberList.get(0);
            } else {
                evil = stageMemberList.stream()
                        .filter(s -> !userIdListAlreadySentSituationCardOnThisRound.contains(s.userId))
                        .findFirst()
                        .get();
            }
            final FlexMessage message = createCard(stageId, roundId, Collections.singletonList(situationCard));
            final String nextYourTurnMessage = messageSource.getMessage(
                    "bot.round.set.round.card.next.your.turn",
                    null,
                    Locale.JAPANESE);
            lineMessage.multicast(
                    Collections.singleton(evil.userId),
                    Arrays.asList(new TextMessage(nextYourTurnMessage), message));

            // send comment and option card to stage member except evil
            HashSet<String> stageMemberExceptEvil = getStageMemberExceptEvil(stageMemberList, evil.userId);
            if (stageMemberExceptEvil.size() > 0) {
                final String gameStartMessage = messageSource.getMessage(
                        "bot.round.set.round.card.game.start",
                        new Object[]{evil.userName},
                        Locale.JAPANESE);
                lineMessage.multicast(
                        stageMemberExceptEvil,
                        Collections.singletonList(new TextMessage(gameStartMessage)));
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

///////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////
///////////////////  private method  //////////////////////
///////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////

    private HashSet<String> getStageMemberExceptEvil(List<StageMember> stageMemberList, String userId) {
        HashSet<String> stageMemberExceptEvil = new HashSet<>();
        for (StageMember member : stageMemberList) {
            if (!member.userId.equals(userId)) {
                stageMemberExceptEvil.add(member.userId);
            }
        }
        return stageMemberExceptEvil;
    }

    private void storeRoundCardAndSendMessage(ReplyToken replyToken, String userId, Long roundId, Card card) {
        RoundCard roundCard = new RoundCard();
        roundCard.id = CommonUtils.getUUID();
        roundCard.roundId = roundId;
        roundCard.userId = userId;
        roundCard.cardId = card.id;
        roundCard.createDate = Timestamp.valueOf(LocalDateTime.now());
        roundCardRepository.save(roundCard);

        final String successMessage = messageSource.getMessage(
                "bot.round.set.round.card.success",
                null,
                Locale.JAPANESE);
        lineMessage.reply(replyToken, Collections.singletonList(new TextMessage(successMessage)));
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

    private void createAndSendCommentMessage(Round round, Set<String> userIds) {
        List<Card> commentList = cardRepository.findByTypeAndIdStartsWithOrderByCreateDate(CardType.COMMENT.name(), round.situationCardId);
        FlexMessage flexMessage = createCard(round.stageId, round.id, commentList);
        final String nextYourTurnMessage = messageSource.getMessage(
                "bot.round.set.round.card.next.your.turn",
                null,
                Locale.JAPANESE);
        lineMessage.multicast(
                userIds,
                Arrays.asList(flexMessage, new TextMessage(nextYourTurnMessage)));
    }

    private boolean createAndSendThemeMessage(Optional<ReplyToken> optionalReplyToken, Round round, String userId) {
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

        if (optionalReplyToken.isPresent()){
            lineMessage.reply(optionalReplyToken.get(),messageList);
        } else {
            lineMessage.multicast(Collections.singleton(userId),messageList);
        }
        return true;
    }

///////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////
///////////////////  private method  //////////////////////
////////////////  to construct message  ///////////////////
///////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////

    private FlexMessage createCard(String stageId, Long roundId, List<Card> cardList) {
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
        return new FlexMessage(altText, carousel);
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
        bodyBox = Box.builder()
                .layout(FlexLayout.VERTICAL)
                .contents(Arrays.asList(typeComponent, separator, textComponent))
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
        return bubble;
    };

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
