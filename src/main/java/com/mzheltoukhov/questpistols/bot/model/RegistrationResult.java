package com.mzheltoukhov.questpistols.bot.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegistrationResult {
    private boolean registered = false;
    private String message;
    private Long invitedToChatId;
    private String registrationCodeMessage;
    private String targetUser;
    private Long targetUserId;
    private String gameName;
}
