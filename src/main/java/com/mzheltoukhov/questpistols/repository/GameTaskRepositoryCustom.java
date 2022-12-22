package com.mzheltoukhov.questpistols.repository;

import com.mzheltoukhov.questpistols.model.GameTask;

public interface GameTaskRepositoryCustom {

    GameTask lockGameTask(String id);
    void unlockGameTask(String id);

}
