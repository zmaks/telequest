package com.mzheltoukhov.questpistols.exception;

public class RegistrationException extends Exception {

    private final MessageType messageType;

    public RegistrationException(MessageType messageType) {
        this.messageType = messageType;
    }

    public static enum MessageType {
        GAME_IS_FULL,
        COMPETITION_IS_FULL,
        GAME_NOT_STARTED_IN_COMPETITION,
    }

    public MessageType getMessageType() {
        return messageType;
    }
}
