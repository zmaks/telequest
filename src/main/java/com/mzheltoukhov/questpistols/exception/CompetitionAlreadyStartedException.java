package com.mzheltoukhov.questpistols.exception;

public class CompetitionAlreadyStartedException extends Exception {
    public CompetitionAlreadyStartedException() {
        super();
    }

    public CompetitionAlreadyStartedException(String message) {
        super(message);
    }
}
