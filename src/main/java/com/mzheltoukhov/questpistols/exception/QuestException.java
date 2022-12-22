package com.mzheltoukhov.questpistols.exception;

public class QuestException extends Exception {
    public QuestException() {
        super();
    }

    public QuestException(String message) {
        super(message);
    }

    public QuestException(String message, Throwable cause) {
        super(message, cause);
    }

    public QuestException(Throwable cause) {
        super(cause);
    }
}
