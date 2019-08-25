package com.slgerkamp.psychological.safety.game.domain.game.service;

import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.profile.UserProfileResponse;
import com.slgerkamp.psychological.safety.game.domain.game.StageMemberStatus;
import com.slgerkamp.psychological.safety.game.infra.message.LineMessage;
import com.slgerkamp.psychological.safety.game.infra.model.Stage;
import com.slgerkamp.psychological.safety.game.infra.model.StageMember;
import com.slgerkamp.psychological.safety.game.infra.model.StageMemberRepository;
import com.slgerkamp.psychological.safety.game.infra.utils.CommonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class StageMemberService {

    private static final Logger log = LoggerFactory.getLogger(StageMemberService.class);

    @Autowired
    private StageMemberRepository stageMemberRepository;
    @Autowired
    private LineMessage lineMessage;
    @Autowired
    private MessageSource messageSource;
    @Autowired
    private NotificationService notificationService;

    public List<StageMember> getStageMemberForStage(final String stageId) {
        List<StageMember> stageMemberList = stageMemberRepository.findByStageId(stageId);
        return stageMemberList;
    }

    public List<StageMember> getStageMemberForDisplayStageMember(final String stageId) {
        List<String> statusList = Arrays.asList(
                StageMemberStatus.JOINING.name(),
                StageMemberStatus.TERMINATED.name());
        List<StageMember> stageMemberList =
                stageMemberRepository.findByStageIdAndStatusIn(stageId, statusList);
        return stageMemberList;
    }

///////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////
///////////////////  default method  //////////////////////
///////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////

    Optional<StageMember> getStageJoiningMemberFromUserId(final String userId) {
        List<StageMember> userStageMemberList = getStageJoiningMembersFromUserId(userId);
        Optional<StageMember> optionalStageMember = Optional.empty();
        if (userStageMemberList.size() > 0 ) {
            optionalStageMember = Optional.of(userStageMemberList.get(0));
        }
        return optionalStageMember;
    }

    List<StageMember> getStageJoiningMembersFromUserId(String userId) {
        return stageMemberRepository.findByUserIdAndStatus(userId, StageMemberStatus.JOINING.name());
    }

    void addMember(final String userId, final String stageId) {
        final UserProfileResponse userProfileResponse = lineMessage.getProfile(userId);
        addMember_doNotCallDirectly(
                stageId,
                userId,
                userProfileResponse.getDisplayName(),
                userProfileResponse.getPictureUrl(),
                StageMemberStatus.JOINING.name());
    }

    List<StageMember> terminatedStageMembers(String stageId) {
        List<StageMember> stageMemberList = getStageMemberForStage(stageId);
        List<StageMember> terminatedStageMemberList = new ArrayList<>();
        for (StageMember stageMember : stageMemberList) {
            stageMember.status = StageMemberStatus.TERMINATED.name();
            terminatedStageMemberList.add(stageMember);
        }
        stageMemberRepository.saveAll(terminatedStageMemberList);
        return stageMemberList;
    }

    void addMemberAndSendMessageToMemberFromWeb(
            final OAuth2Authentication oAuth2Authentication,
            final Stage stage) {
        Map<String, Object> properties =
                (Map<String, Object>) oAuth2Authentication.getUserAuthentication().getDetails();
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

///////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////
///////////////////  private method  //////////////////////
///////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////

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
        notificationService.publishToStompClient(stageId);
    }

    private Optional<StageMember> getStageMemberForEachUser(final String userId) {
        Optional<StageMember> optionalStageMember = Optional.empty();
        List<String> statusList = Arrays.asList(
                StageMemberStatus.APPLY_TO_JOIN.name(),
                StageMemberStatus.JOINING.name());
        List<StageMember> stageMemberList =
                stageMemberRepository.findByUserIdAndStatusIn(userId, statusList);
        if (stageMemberList.size() > 0) {
            optionalStageMember = Optional.of(stageMemberList.get(0));
        }
        return optionalStageMember;
    }

    private void sendMessageForJoiningMember_doNotCallDirectly(
            final String userId,
            final Stage stage) {
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
}
