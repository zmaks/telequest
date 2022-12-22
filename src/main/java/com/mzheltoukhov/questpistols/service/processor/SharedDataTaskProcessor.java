package com.mzheltoukhov.questpistols.service.processor;

import com.mzheltoukhov.questpistols.model.GameMessage;
import com.mzheltoukhov.questpistols.model.GameTask;
import com.mzheltoukhov.questpistols.model.GameTaskAnswer;
import com.mzheltoukhov.questpistols.model.event.GameTaskEvent;
import com.mzheltoukhov.questpistols.model.event.bot.BotEvent;
import com.mzheltoukhov.questpistols.model.task.QuestTaskAnswer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
@Slf4j
public class SharedDataTaskProcessor extends GameTaskProcessor {
    @Override
    protected void processStartedTask(GameTask gameTask, GameMessage gameMessage) {
        if (gameMessage == null || gameMessage.getText() == null || !gameMessage.getText().startsWith(startSymbol)) {
            return;
        }
        String answer = parseAnswer(gameMessage).toLowerCase();
        if (checkIfAlreadyAnswered(answer, gameMessage, gameTask)) {
            log.info("{} - {} ({})| Repeated answer: {}", gameMessage.getChatName(), gameMessage.getChatId(), gameMessage.getUsername(), gameMessage.getText());
            return;
        }
        gameTask.getAnswers().add(new GameTaskAnswer(null, answer, getPlayerName(gameMessage), OffsetDateTime.now(), 0, false));
        gameTaskRepository.save(gameTask);
        log.info("{} - {} ({})| Added shared data answer: {}", gameMessage.getChatName(), gameMessage.getChatId(), gameMessage.getUsername(), gameMessage.getText());
        positiveReaction(gameTask, gameMessage);
        if (isTaskFinished(gameTask)) {
            gameTask.setEndTime(OffsetDateTime.now());
            finishTask(gameTask, null);
            eventPublisher.publishEvent(GameTaskEvent.finished(gameTask.getId()));
        }
    }

    private boolean checkIfAlreadyAnswered(String answer, GameMessage gameMessage, GameTask gameTask) {
        boolean alreadyAnswered = gameTask.getAnswers().stream().anyMatch(a -> a != null && a.getAnswer() != null && a.getAnswer().equalsIgnoreCase(answer));
        if (alreadyAnswered) {
            if (!gameTask.getQuestTask().isIgnoreAlreadyAnswered()) {
                String response = getRandomReaction(getAlreadyAnsweredReactions(gameTask));
                eventPublisher.publishEvent(BotEvent.text(gameTask.getChatId(), response, gameMessage.getMessageId()));
            }
            return true;
        }
        return false;
    }
}
