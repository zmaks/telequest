package com.mzheltoukhov.questpistols.service;

import com.mzheltoukhov.questpistols.model.GameTask;
import com.mzheltoukhov.questpistols.model.GameTaskAnswer;
import com.mzheltoukhov.questpistols.model.GameTaskStatus;
import com.mzheltoukhov.questpistols.model.TaskType;
import com.mzheltoukhov.questpistols.model.task.QuestTask;
import com.mzheltoukhov.questpistols.repository.GameRepository;
import com.mzheltoukhov.questpistols.repository.GameTaskRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SharedDataTaskService {

    private final GameTaskRepository gameTaskRepository;
    private final GameRepository gameRepository;

    @Autowired
    public SharedDataTaskService(GameTaskRepository gameTaskRepository, GameRepository gameRepository) {
        this.gameTaskRepository = gameTaskRepository;
        this.gameRepository = gameRepository;
    }

    @Scheduled(fixedDelay = 30000)
    public void recalculateSharedDataPoints() {
        OffsetDateTime minStartTime = OffsetDateTime.now().minusDays(1);
        Map<String, List<GameTask>> competitionTasks = gameTaskRepository
                .findByStatusAndTypeAndStartTimeAfter(GameTaskStatus.FINISHED, TaskType.SHARED_DATA, minStartTime).stream()
                .collect(Collectors.groupingBy(GameTask::getCompetition));
        Set<String> gameIdsToUpdate = new HashSet<>();
        for (List<GameTask> tasksInCompetition : competitionTasks.values()) {
            Map<String, List<GameTask>> groupOfSharedDataTasksMap = tasksInCompetition.stream().collect(Collectors.groupingBy(t -> t.getQuestTask().getName()));
            for (List<GameTask> groupOfSharedDataTasks : groupOfSharedDataTasksMap.values()) {
                if (groupOfSharedDataTasks.isEmpty()) {
                    continue;
                }
                QuestTask questTask = groupOfSharedDataTasks.get(0).getQuestTask();
                Map<String, Long> answerRating = new LinkedHashMap<>();
                groupOfSharedDataTasks.stream()
                        .flatMap(t -> t.getAnswers().stream())
                        .map(GameTaskAnswer::getAnswer)
                        .filter(StringUtils::isNotEmpty)
                        .map(s -> s.replace("ё", "е"))
                        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                        .entrySet().stream()
                        .filter(e -> e.getValue() > 1)
                        .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                        .forEach(e -> answerRating.put(e.getKey(), e.getValue()));

                Map<String, Integer> answerPointsMap = new HashMap<>();
                int lastRating = -1;
                int currentFine = 0;
                int lastPoints = 0;
                int maxPoints = 10;
                if (questTask.getBaseParams() != null && questTask.getBaseParams().getPoints() != null) {
                    maxPoints = questTask.getBaseParams().getPoints();
                }
                for (Map.Entry<String, Long> answer : answerRating.entrySet()) {
                    int points = lastPoints;
                    if (lastRating != answer.getValue()) {
                        points = maxPoints - currentFine;
                    }
                    answerPointsMap.put(answer.getKey(), points);
                    lastPoints = points;
                    currentFine += questTask.getStepPoint();
                    lastRating = answer.getValue().intValue();
                }

                for (GameTask task : groupOfSharedDataTasks) {
                    int points = task.getPoints();
                    for (GameTaskAnswer answer : task.getAnswers()) {
                        if (StringUtils.isNotEmpty(answer.getAnswer())) {
                            answer.setPoints(answerPointsMap.getOrDefault(answer.getAnswer().replace("ё", "е"), 0));
                        }
                    }
                    int newPoints = task.getAnswers().stream().mapToInt(GameTaskAnswer::getPoints).sum();
                    if (points != newPoints) {
                        task.setPoints(newPoints);
                        gameTaskRepository.save(task);
                        gameIdsToUpdate.add(task.getGameId());
                    }
                }
            }
        }

        for (String gameId : gameIdsToUpdate) {
            gameRepository.findById(gameId).ifPresent(game -> {
                int newPoints = game.getTasks().stream().mapToInt(GameTask::getPoints).sum();
                game.setPoints(newPoints);
                gameRepository.save(game);
            });
        }
    }
}
