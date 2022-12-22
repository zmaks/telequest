package com.mzheltoukhov.questpistols.exception;

public class GameAlreadyExistsException extends Exception {
    public GameAlreadyExistsException() {
        super();
    }

    public GameAlreadyExistsException(String message) {
        super(message);
    }
}
