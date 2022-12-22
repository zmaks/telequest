package com.mzheltoukhov.questpistols.repository;

import com.mzheltoukhov.questpistols.model.GameTask;
import com.mzheltoukhov.questpistols.model.GameTaskStatus;
import com.mzheltoukhov.questpistols.model.TaskType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface GameTaskRepository extends MongoRepository<GameTask, String>, GameTaskRepositoryCustom {
    List<GameTask> findByStatus(GameTaskStatus status);
    List<GameTask> findByStatusAndTypeAndStartTimeAfter(GameTaskStatus status, TaskType type, OffsetDateTime minStartTime);
}
