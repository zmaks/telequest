package com.mzheltoukhov.questpistols;

import com.mzheltoukhov.questpistols.model.Quest;
import com.mzheltoukhov.questpistols.model.QuestBaseParams;
import com.mzheltoukhov.questpistols.model.task.QuestTask;
import com.mzheltoukhov.questpistols.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;

@Component
@ConditionalOnProperty(prefix = "demo", name = "enabled")
public class QuestDemoCreator {

    @Autowired
    private QuestRepository questRepository;

    @Autowired
    private QuestTaskRepository questTaskRepository;

    @Autowired
    private GameTaskRepository gameTaskRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private CompetitionRepository competitionRepository;

    @PostConstruct
    public void init() {
        gameTaskRepository.deleteAll();
        gameRepository.deleteAll();
        competitionRepository.deleteAll();
//        Quest quest = questRepository.findByName("demo");
//        if (quest == null) {
//            quest = new Quest();
//        }
//        quest.setName("demo");
//        quest.setEndMessage("Конец");
//        QuestBaseParams questBaseParams = QuestBaseParams.defaults();
//        questBaseParams.setTimeoutSeconds(3);
//        quest.setBaseParams(questBaseParams);
//
//        QuestTask questTask1 = new QuestTask();
//        questTask1.setName("q1");
//        questTask1.setDescriptions("Вопрос номер один (1)");
//        questTask1.setAnswers(Arrays.asList("1"));
//        questTask1 = saveTask(questTask1);
//
//        QuestTask questTask2 = new QuestTask();
//        questTask2.setName("q2");
//        questTask2.setDescriptions("Вопрос номер два (2)");
//        questTask2.setAnswers(Arrays.asList("2"));
//        questTask2 = saveTask(questTask2);
//
//        QuestTask questTask3 = new QuestTask();
//        questTask3.setName("q3");
//        questTask3.setDescriptions("Вопрос номер два (3)");
//        questTask3.setAnswers(Arrays.asList("3"));
//        questTask3 = saveTask(questTask3);
//
//        quest.setTasks(Arrays.asList(questTask1, questTask2, questTask3));
//        questRepository.save(quest);
    }

//    private QuestTask saveTask(QuestTask questTask) {
//        QuestTask existing = questTaskRepository.findByName(questTask.getName());
//        if (existing != null) {
//            questTask.setId(existing.getId());
//        }
//        return questTaskRepository.save(questTask);
//    }
}
