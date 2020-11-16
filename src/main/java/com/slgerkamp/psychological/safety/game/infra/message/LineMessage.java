package com.slgerkamp.psychological.safety.game.infra.message;

import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.model.Multicast;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.message.Message;
import com.linecorp.bot.model.profile.UserProfileResponse;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@Component
public class LineMessage {

    @Autowired
    private LineMessagingClient lineMessagingClient;

    public UserProfileResponse getProfile(@NonNull String userId){
        try {
            UserProfileResponse userProfileResponse = lineMessagingClient.getProfile(userId).get();
            return userProfileResponse;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public void multicast(@NonNull Set<String> userIds, @NonNull List<Message> messages) {
        try {
            lineMessagingClient.multicast(new Multicast(userIds, messages)).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public void reply(@NonNull ReplyToken replyToken, @NonNull List<Message> messages) {
        try {
            lineMessagingClient.replyMessage(new ReplyMessage(replyToken.value, messages)).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
