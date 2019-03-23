package com.slgerkamp.psychological.safety.game.domain.game;

import com.linecorp.bot.model.action.PostbackAction;
import com.linecorp.bot.model.message.TemplateMessage;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.message.template.CarouselColumn;
import com.linecorp.bot.model.message.template.CarouselTemplate;
import com.linecorp.bot.model.profile.UserProfileResponse;
import com.slgerkamp.psychological.safety.game.application.controller.GameController;
import com.slgerkamp.psychological.safety.game.infra.message.LineMessage;
import com.slgerkamp.psychological.safety.game.infra.model.Stage;
import com.slgerkamp.psychological.safety.game.infra.model.StageMember;
import com.slgerkamp.psychological.safety.game.infra.model.StageMemberRepository;
import com.slgerkamp.psychological.safety.game.infra.model.StageRepository;
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
    private LineMessage lineMessage;

    @Autowired
    private MessageSource messageSource;

    public void createStage(String userId){

        final Stage result_stage = createStage();

        addMember(userId, result_stage.id);

        // send password to creator
        String msg = messageSource.getMessage(
                "bot.stage.create.success",
                new Object[]{result_stage.id, result_stage.password},
                Locale.JAPANESE);
        lineMessage.multicast(Collections.singleton(userId), Collections.singletonList(new TextMessage(msg)));
    }

    public void getStagesParticipantsWanted(String userId) {
        // check whether sender already join a stage or not
        // fetch stagesParticipantsWanted
        final List<Stage> stageList = stageRepository.findParticipantsWantedStageList(PageRequest.of(0, 10));
        log.debug(stageList.stream().map(n -> n.id).collect(Collectors.joining(",")));
        // send them to sender and ask which stage sender want to join
        TemplateMessage templateMessage = createParticipantsWantedStageList(stageList);
        lineMessage.multicast(Collections.singleton(userId), Collections.singletonList(templateMessage));

    }

    public void selectStageToJoin(String userId, String stageId) {
        // check whether sender already join a stage or not
        // fetch stagesParticipantsWanted
        // send them to sender and ask which stage sender want to join
    }

    public void confirmPasswordToJoinAStage(String userId, String password) {
        // check the stage that sender wants to join
        // check the password that sender types for joining the stage that sender selected.
        // add member to the stage that sender selected
        // send message about "add member" to stage members
        // check the number of stage members and if it's more than 6, stage status is changed and send message about that
    }

    private void changeStageStatus() {
        //
    }


    private Stage createStage() {
        final String stageId = CommonUtils.getUUID();
        final String password = CommonUtils.get6DigitCode();
        final Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        final Stage stage = new Stage();
        stage.id = stageId;
        stage.password = password;
        stage.createDate = now;
        stage.status = StageStatus.PARTICIPANTS_WANTED.status;
        stage.updateDate = now;
        return stageRepository.save(stage);
    }

    private void addMember(String userId, String stageId) {
        final UserProfileResponse userProfileResponse = lineMessage.getProfile(userId);
        final StageMember stageMember = new StageMember();
        stageMember.stageId = stageId;
        stageMember.userId = userProfileResponse.getUserId();
        stageMember.userName = userProfileResponse.getDisplayName();
        stageMemberRepository.save(stageMember);
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
            log.debug("postbackData = " + postbackData);

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
}
