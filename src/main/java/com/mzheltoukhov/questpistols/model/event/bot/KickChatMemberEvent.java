package com.mzheltoukhov.questpistols.model.event.bot;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class KickChatMemberEvent {
    private Long chatId;
    private Integer userId;
}
