package com.mzheltoukhov.questpistols.model;

import lombok.Data;

@Data
public class Invite {
    private Long invitedToChat;
    private String gameName;
    private String registrationCode;
    private Competition competition;

    public Invite() {
    }

    public Invite(Long invitedToChat, String registrationCode, String gameName, Competition competition) {
        this.invitedToChat = invitedToChat;
        this.registrationCode = registrationCode;
        this.gameName = gameName;
        this.competition = competition;
    }
}
