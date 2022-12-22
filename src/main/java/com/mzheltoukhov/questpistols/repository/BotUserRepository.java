package com.mzheltoukhov.questpistols.repository;

import com.mzheltoukhov.questpistols.bot.model.BotUser;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BotUserRepository extends MongoRepository<BotUser, String> {
    BotUser findByUserId(Long userId);
}
