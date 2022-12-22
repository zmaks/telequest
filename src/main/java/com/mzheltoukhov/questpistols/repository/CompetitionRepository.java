package com.mzheltoukhov.questpistols.repository;

import com.mzheltoukhov.questpistols.model.Competition;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompetitionRepository extends MongoRepository<Competition, String> {
    Competition findByName(String name);
    Competition findTopByEnabledIsTrueOrderByCreatedTimeDesc();
    Competition findFirstByCodePhrase(String codePhrase);
}
