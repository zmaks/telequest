package com.mzheltoukhov.questpistols.repository;

import com.mzheltoukhov.questpistols.model.Game;
import com.mzheltoukhov.questpistols.model.GameStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface GameRepository extends MongoRepository<Game, String>, GameRepositoryCustom {
    Game findByChatIdAndStatus(Long chatId, GameStatus status);
    List<Game> findByChatId(Long chatId);
    List<Game> findByTooManyPlayersIsTrue();
    List<Game> findByCreatedAfter(OffsetDateTime time);
    Game findFirstByRegistrationCode(String registrationCode);
    List<Game> findByCompetitionAndDisabledIsFalse(String competition);
    boolean existsByChatIdAndCompetition(Long chatId, String competition);
}
