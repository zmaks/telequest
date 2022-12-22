package com.mzheltoukhov.questpistols.service.processor;

import com.mzheltoukhov.questpistols.model.GameTask;
import com.mzheltoukhov.questpistols.model.Quest;
import com.mzheltoukhov.questpistols.model.QuestBaseParams;
import com.mzheltoukhov.questpistols.model.task.QuestTask;
import com.mzheltoukhov.questpistols.model.task.QuestTaskAnswer;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SimpleQuestionTaskProcessorTest {

    @Test
    public void testPointCalculation() {
        SimpleQuestionTaskProcessor processor = new SimpleQuestionTaskProcessor();
        OffsetDateTime now = OffsetDateTime.now();
        GameTask gameTask =GameTask.builder()
                .startTime(now.minusSeconds(5))
                .questTask(getQuestTask(100, 100, 10))
                .build();

        Integer points = processor.getPoints(gameTask, null, now);

        assertEquals(50, points);
    }

    @Test
    public void testPointCalculation_withScale() {
        SimpleQuestionTaskProcessor processor = new SimpleQuestionTaskProcessor();
        OffsetDateTime now = OffsetDateTime.now();
        GameTask gameTask =GameTask.builder()
                .startTime(now.minusSeconds(7))
                .questTask(getQuestTask(100, 100, 15))
                .build();

        Integer points = processor.getPoints(gameTask, null, now);

        assertEquals(53, points);
    }

    @Test
    public void testPointCalculation_zeroSpentTime() {
        SimpleQuestionTaskProcessor processor = new SimpleQuestionTaskProcessor();
        OffsetDateTime now = OffsetDateTime.now();
        GameTask gameTask =GameTask.builder()
                .startTime(now)
                .questTask(getQuestTask(100, 100, 15))
                .build();

        Integer points = processor.getPoints(gameTask, null, now);

        assertEquals(100, points);
    }

    @Test
    public void testPointCalculation_longSpentTime() {
        SimpleQuestionTaskProcessor processor = new SimpleQuestionTaskProcessor();
        OffsetDateTime now = OffsetDateTime.now();
        GameTask gameTask =GameTask.builder()
                .startTime(now.minusSeconds(17))
                .questTask(getQuestTask(100, 100, 15))
                .build();

        Integer points = processor.getPoints(gameTask, null, now);

        assertEquals(0, points);
    }

    @Test
    public void testPointCalculation_nullFinePoints() {
        SimpleQuestionTaskProcessor processor = new SimpleQuestionTaskProcessor();
        OffsetDateTime now = OffsetDateTime.now();
        GameTask gameTask =GameTask.builder()
                .startTime(now.minusSeconds(5))
                .questTask(getQuestTask(100, null, 15))
                .build();

        Integer points = processor.getPoints(gameTask, null, now);

        assertEquals(100, points);
    }

    @Test
    public void testPointCalculation_nullPointsTime() {
        SimpleQuestionTaskProcessor processor = new SimpleQuestionTaskProcessor();
        OffsetDateTime now = OffsetDateTime.now();
        GameTask gameTask =GameTask.builder()
                .startTime(now.minusSeconds(5))
                .questTask(getQuestTask(100, 100, null))
                .build();

        Integer points = processor.getPoints(gameTask, null, now);

        assertEquals(100, points);
    }

    @Test
    public void testPointCalculation_fromQuestBaseParams() {
        SimpleQuestionTaskProcessor processor = new SimpleQuestionTaskProcessor();
        OffsetDateTime now = OffsetDateTime.now();
        GameTask gameTask =GameTask.builder()
                .startTime(now.minusSeconds(6))
                .quest(getQuest(100, 60, 18))
                .build();

        Integer points = processor.getPoints(gameTask, null, now);

        assertEquals(80, points);
    }

    @Test
    public void testPointCalculation_fromQuestTaskAnswer() {
        SimpleQuestionTaskProcessor processor = new SimpleQuestionTaskProcessor();
        OffsetDateTime now = OffsetDateTime.now();
        GameTask gameTask =GameTask.builder()
                .startTime(now.minusSeconds(10))
                .build();

        Integer points = processor.getPoints(gameTask, getQuestTaskAnswer(100, 79, 5), now);

        assertEquals(21, points);
    }

    @Test
    public void testPointCalculation_fromSeveralSources() {
        SimpleQuestionTaskProcessor processor = new SimpleQuestionTaskProcessor();
        OffsetDateTime now = OffsetDateTime.now();
        GameTask gameTask =GameTask.builder()
                .startTime(now.minusSeconds(9))
                .quest(getQuest(101, null, null))
                .questTask(getQuestTask(null, 73, null))
                .build();

        Integer points = processor.getPoints(gameTask, getQuestTaskAnswer(null, null, 46), now);

        assertEquals(87, points);
    }

    private QuestTask getQuestTask(Integer points, Integer finePoints, Integer pointsTime) {
        QuestTask questTask = new QuestTask();
        questTask.setBaseParams(getQuestBaseParams(points, finePoints, pointsTime));
        return questTask;
    }

    private Quest getQuest(Integer points, Integer finePoints, Integer pointsTime) {
        Quest quest = new Quest();
        quest.setBaseParams(getQuestBaseParams(points, finePoints, pointsTime));
        return quest;
    }

    private QuestTaskAnswer getQuestTaskAnswer(Integer points, Integer finePoints, Integer pointsTime) {
        QuestTaskAnswer answer = new QuestTaskAnswer();
        answer.setPoints(points);
        answer.setFinePoints(finePoints);
        answer.setPointsTime(pointsTime);
        return answer;
    }

    private QuestBaseParams getQuestBaseParams(Integer points, Integer finePoints, Integer pointsTime) {
        return QuestBaseParams.builder()
                .points(points)
                .finePoints(finePoints)
                .pointsTimeSeconds(pointsTime)
                .build();
    }
}
