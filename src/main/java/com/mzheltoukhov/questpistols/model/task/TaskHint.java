package com.mzheltoukhov.questpistols.model.task;

import lombok.Data;

import java.util.List;

@Data
public class TaskHint {

    private String hintKeyWord;
    private String hintAnnounceText;
    private Integer hintTimeInSeconds;
    private Integer hintFinePoints;
    private List<ResponsePayload> descriptions;
}
