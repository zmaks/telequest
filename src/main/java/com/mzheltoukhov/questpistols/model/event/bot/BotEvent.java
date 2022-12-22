package com.mzheltoukhov.questpistols.model.event.bot;

import com.mzheltoukhov.questpistols.model.PayloadType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BotEvent {
    private Long chatId;
    private Integer replyTo;
    private String message;
    private PayloadType payloadType = PayloadType.TEXT;
    private String fileId;
    private String resourceName;
    private BotEventButton button;
    private List<BotEventButton> buttons;
    private List<BotEventPayload> payloadGroup;

    private boolean pin;
    private boolean unPin;

    public static BotEvent text(Long chatId, String message) {
        return BotEvent.builder().chatId(chatId).message(message).payloadType(PayloadType.TEXT).build();
    }

    public static BotEvent text(Long chatId, String message, Integer replyToMessageId) {
        return BotEvent.builder().chatId(chatId).message(message).payloadType(PayloadType.TEXT).replyTo(replyToMessageId).build();
    }

}
