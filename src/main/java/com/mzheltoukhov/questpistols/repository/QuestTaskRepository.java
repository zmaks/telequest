package com.mzheltoukhov.questpistols.repository;

import com.mzheltoukhov.questpistols.model.task.QuestTask;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestTaskRepository extends MongoRepository<QuestTask, String> {
    QuestTask findByNameAndQuestName(String name, String questName);
}
