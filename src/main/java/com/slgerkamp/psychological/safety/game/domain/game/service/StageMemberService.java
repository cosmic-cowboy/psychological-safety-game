package com.slgerkamp.psychological.safety.game.domain.game.service;

import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.profile.UserProfileResponse;
import com.slgerkamp.psychological.safety.game.domain.game.StageMemberRole;
import com.slgerkamp.psychological.safety.game.domain.game.StageMemberStatus;
import com.slgerkamp.psychological.safety.game.infra.message.LineMessage;
import com.slgerkamp.psychological.safety.game.infra.model.Stage;
import com.slgerkamp.psychological.safety.game.infra.model.StageMember;
import com.slgerkamp.psychological.safety.game.infra.model.StageMemberRepository;
import com.slgerkamp.psychological.safety.game.infra.utils.CommonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class StageMemberService {

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

    Optional<StageMember> getActiveStageCreatorFromUserId(final String userId) {
        List<StageMember> stageCreatorList =
                stageMemberRepository.findByUserIdAndRoleAndStatus(
                        userId, StageMemberRole.CREATOR.name(), StageMemberStatus.JOINING.name());
        Optional<StageMember> optionalStageCreator = Optional.empty();
        if (stageCreatorList.size() > 0 ) {
            optionalStageCreator = Optional.of(stageCreatorList.get(0));
        }
        return optionalStageCreator;
    }


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

    void addCreator(final String userId, final String stageId) {
        final UserProfileResponse userProfileResponse = lineMessage.getProfile(userId);
        addMember_doNotCallDirectly(
                stageId,
                userId,
                userProfileResponse.getDisplayName(),
                userProfileResponse.getPictureUrl(),
                StageMemberStatus.JOINING.name(),
                StageMemberRole.CREATOR.name());
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
                StageMemberStatus.JOINING.name(),
                StageMemberRole.PARTICIPATOR.name());

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
            final String status,
            final String role) {

        List<String> statusList = Arrays.asList(
                StageMemberStatus.APPLY_TO_JOIN.name(),
                StageMemberStatus.JOINING.name());

        List<StageMember> stageMemberList =
                stageMemberRepository.findByUserIdAndStatusIn(userId, statusList);

        if (stageMemberList.size() > 0) {
            List<String> stageList = stageMemberList.stream().map(s -> s.stageId).collect(Collectors.toList());
            sendMessageForRemoveStageMemberFromJoining(userId, stageList);
            stageMemberRepository.deleteByUserIdAndStatusIn(userId, statusList);
        }
        final StageMember stageMember = new StageMember();
        stageMember.id = CommonUtils.getUUID();
        stageMember.stageId = stageId;
        stageMember.userId = userId;
        stageMember.userName = displayName;
        stageMember.pictureUrl = pictureUrl;
        stageMember.status = status;
        stageMember.role = role;
        stageMember.createDate = Timestamp.valueOf(LocalDateTime.now());
        stageMemberRepository.save(stageMember);
        notificationService.publishToStompClient(stageId);
    }

    private void sendMessageForJoiningMember_doNotCallDirectly(
            final String userId,
            final Stage stage) {
        final String url = CommonUtils.createStageUrl(stage.id);
        final String joinNewStage = messageSource.getMessage(
                "bot.stage.input.join.new.stage",
                new Object[]{stage.id, url},
                Locale.JAPANESE);
        lineMessage.multicast(
                Collections.singleton(userId),
                Collections.singletonList(new TextMessage(joinNewStage)));
    }

    private void sendMessageForRemoveStageMemberFromJoining(
            final String userId,
            final List<String> stageList) {
        final String deleteStageBeforeJoining = messageSource.getMessage(
                "bot.stage.input.join.delete.stage",
                new Object[]{String.join(",", stageList)},
                Locale.JAPANESE);
        lineMessage.multicast(
                Collections.singleton(userId),
                Collections.singletonList(new TextMessage(deleteStageBeforeJoining)));
    }

}
