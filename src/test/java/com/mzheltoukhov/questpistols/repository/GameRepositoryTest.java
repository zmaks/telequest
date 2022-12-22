package com.mzheltoukhov.questpistols.repository;

import com.mzheltoukhov.questpistols.configuration.mongo.MongoConfiguration;
import com.mzheltoukhov.questpistols.model.Game;
import com.mzheltoukhov.questpistols.model.Player;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataMongoTest
@Import(MongoConvertersConfig.class)
@ExtendWith(SpringExtension.class)
public class GameRepositoryTest {

    @Autowired
    private GameRepository gameRepository;


}
