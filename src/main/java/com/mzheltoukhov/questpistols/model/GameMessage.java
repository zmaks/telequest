package com.mzheltoukhov.questpistols.model;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.Arrays;

@Data
@Builder
public class GameMessage {
    private Long chatId;
    private boolean groupChat;
    private String chatName;
    private String username;
    private String userTitle;
    private Integer messageId;
    private String text;
    private GameMessagePayload payload;
    private String callBackAnswer;
    private Player player;

    public static GameMessage fromTelegramMessage(Message message) {

        return GameMessage.builder()
                .chatId(message.getChatId())
                .groupChat(message.getChat().isGroupChat())
                .username(getFrom(message.getFrom()))
                .userTitle(getUserTitle(message.getFrom()))
                .player(new Player(message.getFrom()))
                .chatName(getChatName(message))
                .messageId(message.getMessageId())
                .text(message.getText())
                .payload(getMessagePayload(message))
                .build();
    }

    private static String getUserTitle(User user) {
        if (user == null) {
            return "unknown";
        }
        String firstName = StringUtils.defaultString(user.getFirstName());
        String lastName = StringUtils.defaultString(user.getLastName());
        return String.join(" ", Arrays.asList(firstName, lastName)).trim();
    }

    public static GameMessage fromTelegramCallback(CallbackQuery callbackQuery) {
        return GameMessage.builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .groupChat(callbackQuery.getMessage().isGroupMessage())
                .username(getFrom(callbackQuery.getFrom()))
                .userTitle(getUserTitle(callbackQuery.getFrom()))
                .player(new Player(callbackQuery.getFrom()))
                .chatName(getChatName(callbackQuery.getMessage()))
                .callBackAnswer(callbackQuery.getData())
                .build();
    }

    private static String getFrom(User user) {
        if (user == null) {
            return "unknown";
        }
        if (user.getUserName() != null) {
            return user.getUserName();
        }
        String firstName = StringUtils.defaultString(user.getFirstName());
        String lastName = StringUtils.defaultString(user.getLastName());
        return String.join(" ", Arrays.asList(firstName, lastName)).trim();
    }

    private static String getChatName(Message message) {
        String chatName = message.getChat().getTitle();
        if (StringUtils.isBlank(chatName)) {
            String firstName = StringUtils.defaultString(message.getChat().getFirstName());
            String lastName = StringUtils.defaultString(message.getChat().getLastName());
            chatName = String.join(" ", Arrays.asList(firstName, lastName)).trim();
        }
        if (StringUtils.isBlank(chatName)) {
            chatName = message.getChat().getUserName();
        }
        return chatName;
    }

    private static GameMessagePayload getMessagePayload(Message message) {
        GameMessagePayload payload = null;
        if (message.hasPhoto()) {
            payload = new GameMessagePayload(PayloadType.PHOTO, message.getPhoto().get(0).getFileId());
        }
        if (message.hasVideo()) {
            payload = new GameMessagePayload(PayloadType.VIDEO, message.getVideo().getFileId());
        }
        if (message.hasVoice()) {
            payload = new GameMessagePayload(PayloadType.VOICE, message.getVoice().getFileId());
        }
        if (message.hasDocument()) {
            payload = new GameMessagePayload(PayloadType.DOCUMENT, message.getDocument().getFileId());
        }
        if (message.hasAnimation()) {
            payload = new GameMessagePayload(PayloadType.ANIMATION, message.getAnimation().getFileId());
        }
        if (message.hasSticker()) {
            payload = new GameMessagePayload(PayloadType.STICKER, message.getSticker().getFileId());
        }
        return payload;
    }
}
