package com.mzheltoukhov.questpistols.service;

import com.mzheltoukhov.questpistols.model.*;
import com.mzheltoukhov.questpistols.model.task.QuestTask;
import com.mzheltoukhov.questpistols.repository.CompetitionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CompetitionServiceTest {

    @Mock
    private GameService gameService;
    @Mock
    private QuestService questService;
    @Mock
    private CompetitionRepository competitionRepository;
    @InjectMocks
    private CompetitionService competitionService;

    @Test
    public void test() {
        Player player1 = new Player();
        player1.setId(1L);
        Player player2 = new Player();
        player2.setId(2L);
        player2.setLastAnswer(OffsetDateTime.now().minusDays(2));
        Player player3 = new Player();
        player3.setId(3L);
        player3.setLastAnswer(OffsetDateTime.now());
        Player player = List.of(player1, player2, player3).stream()
                .min(Comparator.comparing(Player::getLastAnswer, Comparator.nullsFirst(Comparator.naturalOrder()))).get();
        assertEquals(1, player.getId());
    }

    @Disabled
    @Test
    public void testExcelGeneration() throws Exception {
        var quest = new Quest();
        quest.setName("quest");
        List<QuestTask> questTasks = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            var questTask = new QuestTask();
            questTask.setName("Quest task " + new Random().nextInt(1000000));
            questTasks.add(questTask);
        }
        quest.setTasks(questTasks);

        var competition = new Competition();
        competition.setName("comp");
        competition.setQuestName("quest");
        List<Game> games = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            var game = new Game();
            game.setPoints(new Random().nextInt(500));
            game.setName("Team number " + new Random().nextInt(1000) * 100);
            List<GameTask> gameTasks = new ArrayList<>();
            OffsetDateTime time = OffsetDateTime.now();
            for (var questTask : questTasks) {
                var gameTask = GameTask.builder().startTime(time).endTime(time.plusSeconds(new Random().nextInt(1000) + 100)).points(new Random().nextInt(100) * 100).build();
                gameTasks.add(gameTask);
            }
            game.setTasks(gameTasks);
            games.add(game);
        }
        competition.setGames(games);

        when(competitionRepository.findByName("comp")).thenReturn(competition);
        when(questService.findByName("quest")).thenReturn(quest);

        ByteArrayOutputStream byteArrayOutputStream = competitionService.generateExcelReport("comp");

        try(OutputStream outputStream = new FileOutputStream("result.xls")) {
            byteArrayOutputStream.writeTo(outputStream);
        }

    }
}
