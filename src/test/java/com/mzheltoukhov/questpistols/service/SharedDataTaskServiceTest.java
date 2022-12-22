package com.mzheltoukhov.questpistols.service;

import com.mzheltoukhov.questpistols.model.GameTask;
import com.mzheltoukhov.questpistols.model.GameTaskAnswer;
import com.mzheltoukhov.questpistols.model.GameTaskStatus;
import com.mzheltoukhov.questpistols.model.QuestBaseParams;
import com.mzheltoukhov.questpistols.model.task.QuestTask;
import com.mzheltoukhov.questpistols.repository.GameRepository;
import com.mzheltoukhov.questpistols.repository.GameTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SharedDataTaskServiceTest {

    @Mock
    private GameTaskRepository gameTaskRepository;
    @Mock
    private GameRepository gameRepository;

    @InjectMocks
    private SharedDataTaskService sharedDataTaskService;

    @Test
    public void recalculateSharedDataPoints() {
        QuestTask questTask = new QuestTask();
        questTask.setName("Task");
        questTask.setBaseParams(QuestBaseParams.builder().points(20).build());
        questTask.setStepPoint(2);

        GameTask gameTask1 = GameTask.builder().questTask(questTask).gameId("game1").status(GameTaskStatus.FINISHED).competition("comp")
                .answers(createAnswers("yes1", "yes2", "no1", "no2", "no3")).build();
        GameTask gameTask2 = GameTask.builder().questTask(questTask).gameId("game2").status(GameTaskStatus.FINISHED).competition("comp")
                .answers(createAnswers("yes1", "yes2", "yes3", "no5", "yes4")).build();
        GameTask gameTask3 = GameTask.builder().questTask(questTask).gameId("game3").status(GameTaskStatus.FINISHED).competition("comp")
                .answers(createAnswers("yes1", "yes2", "no6", "no7", "no8")).build();
        GameTask gameTask4 = GameTask.builder().questTask(questTask).gameId("game4").status(GameTaskStatus.FINISHED).competition("comp")
                .answers(createAnswers("yes3", "yes4", "no10", "no11", "no12")).build();
        GameTask gameTask5 = GameTask.builder().questTask(questTask).gameId("game5").status(GameTaskStatus.FINISHED).competition("comp")
                .answers(createAnswers("yes1", "yes3", "no15", "no16", "no17")).build();
        GameTask gameTask6 = GameTask.builder().questTask(questTask).gameId("game6").status(GameTaskStatus.FINISHED).competition("comp")
                .answers(createAnswers("123", "ye12s2", "12", "42", "55")).build();

        when(gameTaskRepository.findByStatusAndTypeAndStartTimeAfter(any(), any(), any())).thenReturn(List.of(gameTask1, gameTask2, gameTask3, gameTask4, gameTask5, gameTask6));
        when(gameRepository.findById(any())).thenReturn(Optional.empty());

        sharedDataTaskService.recalculateSharedDataPoints();

        ArgumentCaptor<GameTask> taskArgumentCaptor = ArgumentCaptor.forClass(GameTask.class);
        verify(gameTaskRepository, times(5)).save(taskArgumentCaptor.capture());
        assertEquals(20 + 18, taskArgumentCaptor.getAllValues().get(0).getPoints());
        assertEquals(20 + 18 + 18 + 14, taskArgumentCaptor.getAllValues().get(1).getPoints());
        assertEquals(20 + 18, taskArgumentCaptor.getAllValues().get(2).getPoints());
        assertEquals(18 + 14, taskArgumentCaptor.getAllValues().get(3).getPoints());
        assertEquals(20 + 18, taskArgumentCaptor.getAllValues().get(4).getPoints());

    }

    private List<GameTaskAnswer> createAnswers(String... answers) {
        return Arrays.stream(answers).map(a -> new GameTaskAnswer(null, a, null, null, 0, false)).collect(Collectors.toList());
    }
}
