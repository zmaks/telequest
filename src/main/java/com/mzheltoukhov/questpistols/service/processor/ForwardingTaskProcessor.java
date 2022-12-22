package com.mzheltoukhov.questpistols.service.processor;

import com.mzheltoukhov.questpistols.model.GameMessage;
import com.mzheltoukhov.questpistols.model.GameTask;
import com.mzheltoukhov.questpistols.model.GameTaskAnswer;
import com.mzheltoukhov.questpistols.model.event.GameTaskEvent;
import com.mzheltoukhov.questpistols.model.event.bot.ForwardedMessageBotEvent;
import com.mzheltoukhov.questpistols.model.task.QuestTaskAnswer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class ForwardingTaskProcessor extends GameTaskProcessor {

    @Override
    public void processStartedTask(GameTask gameTask, GameMessage gameMessage) {
        if (gameMessage.getPayload() == null) {
            return;
        }
        QuestTaskAnswer questTaskAnswer = gameTask.getQuestTask().getAnswers().stream()
                .filter(answer -> answer.getAnswerPayloadType().equals(gameMessage.getPayload().getType()))
                .findAny().orElse(null);

        if (questTaskAnswer == null) {
            negativeReaction(gameTask, gameMessage);
            log.info("{} - {} ({})| Incorrect answer file type: {}", gameMessage.getChatName(), gameMessage.getChatId(), gameMessage.getUsername(), gameMessage.getPayload().getType());
            return;
        }

        GameTaskAnswer gameTaskAnswer = new GameTaskAnswer(null, gameMessage.getPayload().getFileId(), getPlayerName(gameMessage), OffsetDateTime.now(), 0, false);
        gameTask.getAnswers().add(gameTaskAnswer);

        Integer points = getPoints(gameTask, questTaskAnswer, OffsetDateTime.now());
        int currentPoints = gameTask.getPoints();
        currentPoints = currentPoints + points;
        gameTask.setPoints(currentPoints);

        gameTaskRepository.save(gameTask);
        positiveReaction(gameTask, questTaskAnswer, gameMessage);

        CompletableFuture.runAsync(() -> {
            log.info("Forwarding photo to the admins");
                    for (Long frowardTo : gameTask.getQuestTask().getForwardingTargetUsers()) {

                        ForwardedMessageBotEvent event = new ForwardedMessageBotEvent();
                        event.setForwardedMessageId(gameMessage.getMessageId());
                        event.setForwardToChatId(frowardTo);
                        event.setFromChatId(gameTask.getChatId());
                        event.setPayload(gameMessage.getPayload());
                        event.setComment(String.format("Получено от пользователя %s в чате '%s'", gameMessage.getUsername(), gameMessage.getChatName()));
                        eventPublisher.publishEvent(event);
                    }
                });
        log.info("{} - {} ({})| Correct answer: {}", gameMessage.getChatName(), gameMessage.getChatId(), gameMessage.getUsername(), gameMessage.getPayload().getType());

        if (gameTask.getAnswers().size() == gameTask.getQuestTask().getRequiredCorrectAnswers()) {
            gameTask.setEndTime(OffsetDateTime.now());
            finishTask(gameTask, null);
            eventPublisher.publishEvent(GameTaskEvent.finished(gameTask.getId()));
        }
    }
}
