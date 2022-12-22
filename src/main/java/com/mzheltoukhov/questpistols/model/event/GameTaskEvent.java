package com.mzheltoukhov.questpistols.model.event;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GameTaskEvent {
    private String gameTaskId;
    private boolean taskFinished;

    public static GameTaskEvent finished(String gameTaskId) {
        return GameTaskEvent.builder().gameTaskId(gameTaskId).taskFinished(true).build();
    }
}
