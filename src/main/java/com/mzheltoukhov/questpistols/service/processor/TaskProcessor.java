package com.mzheltoukhov.questpistols.service.processor;

import com.mzheltoukhov.questpistols.model.GameMessage;
import com.mzheltoukhov.questpistols.model.GameTask;

public interface TaskProcessor {
    void processTask(GameTask gameTask, GameMessage gameMessage);
    void updateState(GameTask gameTask);
}
