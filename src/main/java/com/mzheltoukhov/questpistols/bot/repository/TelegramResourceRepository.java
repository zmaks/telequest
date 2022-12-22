package com.mzheltoukhov.questpistols.bot.repository;

import com.mzheltoukhov.questpistols.bot.model.TelegramResource;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TelegramResourceRepository extends MongoRepository<TelegramResource, String> {
    TelegramResource findByName(String name);
}
