package com.mzheltoukhov.questpistols.model.event.bot;

import com.mzheltoukhov.questpistols.model.GameMessagePayload;
import lombok.Data;

@Data
public class ForwardedMessageBotEvent {
    private String comment;
    private Long fromChatId;
    private Long forwardToChatId;
    private Integer forwardedMessageId;
    private GameMessagePayload payload;
}
