package com.mzheltoukhov.questpistols.repository;

import com.mzheltoukhov.questpistols.model.Quest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestRepository extends MongoRepository<Quest, String> {
    Quest findByName(String name);
    List<Quest> findByKeyWordsContains(String keyWord);
}
