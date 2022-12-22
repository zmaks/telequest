package com.mzheltoukhov.questpistols.service.processor;

import com.mzheltoukhov.questpistols.model.*;
import com.mzheltoukhov.questpistols.model.event.GameTaskEvent;
import com.mzheltoukhov.questpistols.model.event.bot.BotEvent;
import com.mzheltoukhov.questpistols.model.task.QuestTaskAnswer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
public class SimpleQuestionTaskProcessor extends GameTaskProcessor {
    public static final String SKIP_BONUS_KEY = "пропустить";

    @Override
    public void processStartedTask(GameTask gameTask, GameMessage gameMessage) {
        if (gameMessage == null ) {
            return;
        }
        if (gameMessage.getText() != null && !gameMessage.getText().startsWith(startSymbol)) {
            return;
        }
        String answer = parseAnswer(gameMessage);
        if (answer == null && gameMessage.getCallBackAnswer() != null) {
            answer = checkAndParserCallbackAnswer(gameMessage, gameTask);
        }
        if (answer == null) {
            log.warn("Received a message without any payload. Game task: {}, Game message: {}", gameTask.getId(), gameMessage);
            return;
        }
        if (gameTask.getPlayerToAnswer() != null && gameMessage.getPlayer() != null) {
            if (!gameTask.getPlayerToAnswer().getId().equals(gameMessage.getPlayer().getId())) {
                if (!gameTask.isPlayerToAnswerRemindSent()) {
                    eventPublisher.publishEvent(
                            BotEvent.text(gameTask.getChatId(),
                                    "❌ На этот вопрос отвечает " + gameTask.getPlayerToAnswer().getMention() +
                                            "! \uD83D\uDE42", gameMessage.getMessageId()));
                    gameTask.setPlayerToAnswerRemindSent(true);
                    gameTaskRepository.save(gameTask);
                }
                return;
            }
        }
        if (answer.equals(SKIP_BONUS_KEY) && getNotAnswered(gameTask).stream().allMatch(QuestTaskAnswer::isBonus)
                && gameTask.getQuestTask().isAllowSkippingBonus()) {
            gameTask.setEndTime(OffsetDateTime.now());
            finishTask(gameTask, null);
            eventPublisher.publishEvent(GameTaskEvent.finished(gameTask.getId()));
            return;
        }
        QuestTaskAnswer questTaskAnswer = findSuitableQuestTaskAnswer(answer, gameTask.getQuestTask().getAnswers());
        if (questTaskAnswer != null) {
            boolean alreadyAnswered = checkIfAlreadyAnswered(questTaskAnswer, gameMessage, gameTask);
            if (alreadyAnswered) {
                return;
            }
            checkUsersAnswer(answer, questTaskAnswer, gameMessage, gameTask);
            log.info("{} - {} ({})| Correct answer: {}. Task points: {}",
                    gameMessage.getChatName(), gameMessage.getChatId(), gameMessage.getUsername(),
                    gameMessage.getText(), gameTask.getPoints());
            List<QuestTaskAnswer> notAnswered = getNotAnswered(gameTask);
            if (gameTask.getQuestTask().isAllowSkippingBonus() && notAnswered.stream().anyMatch(QuestTaskAnswer::isBonus)) {
                if (notAnswered.stream().allMatch(QuestTaskAnswer::isBonus)) {

                    String text = String.format("У вас остались неотвеченными только *бонусы* (%s шт).\n" +
                            "Если вы хотите их пропустить и перейти к следующему заданию, пришлите команду *!" + SKIP_BONUS_KEY + "*.\n" +
                            "Время идет ⏱\uD83D\uDE09", notAnswered.size());
                    eventPublisher.publishEvent(BotEvent.text(gameTask.getChatId(), text));
                    return;
                }
            }
            if (isTaskFinished(gameTask) || questTaskAnswer.isFinalAnswer()) {
                gameTask.setEndTime(OffsetDateTime.now());
                finishTask(gameTask, null);
                eventPublisher.publishEvent(GameTaskEvent.finished(gameTask.getId()));
                return;
            }
            return;
        }
        negativeReaction(gameTask, gameMessage);
        gameTask.setAttempts(gameTask.getAttempts() + 1);
        if (gameTask.getQuestTask().getAttemptFine() != null) {
            gameTask.setPoints(gameTask.getPoints() - gameTask.getQuestTask().getAttemptFine());
        }
        gameTaskRepository.save(gameTask);
        log.info("{} - {} ({})| Incorrect answer: {}", gameMessage.getChatName(), gameMessage.getChatId(),
                gameMessage.getUsername(), gameMessage.getText());

        if (gameTask.getQuestTask().getAttempts() != null && gameTask.getQuestTask().getAttempts() == gameTask.getAttempts()) {
            gameTask.setEndTime(OffsetDateTime.now());
            String reaction = getAttemptsExhaustedReaction(gameTask);
            BotEvent botEvent = null;
            if (reaction != null) {
                botEvent = BotEvent.text(gameTask.getChatId(), reaction);
            }
            finishTask(gameTask, botEvent);
            eventPublisher.publishEvent(GameTaskEvent.finished(gameTask.getId()));
        }
    }

    private void checkUsersAnswer(String answer, QuestTaskAnswer questTaskAnswer, GameMessage gameMessage, GameTask gameTask) {
        if (!questTaskAnswer.isIgnoreInCounting()) {
            Integer points = getPoints(gameTask, questTaskAnswer, OffsetDateTime.now());
            gameTask.getAnswers().add(new GameTaskAnswer(questTaskAnswer.getId(), answer, getPlayerName(gameMessage),
                    OffsetDateTime.now(), points, questTaskAnswer.isBonus()));
            if (points != null) {
                int currentPoints = gameTask.getPoints();
                currentPoints = currentPoints + points;
                gameTask.setPoints(currentPoints);
            }
            Game game = gameRepository.findById(gameTask.getGameId()).get();
            for (Player player : game.getPlayers()) {
                if (gameMessage.getPlayer() != null && player.getId().equals(gameMessage.getPlayer().getId())) {
                    player.setLastAnswer(OffsetDateTime.now());
                }
            }
            gameRepository.save(game);
            gameTaskRepository.save(gameTask);
        }

        positiveReaction(gameTask, questTaskAnswer, gameMessage);
    }

    private QuestTaskAnswer findSuitableQuestTaskAnswer(String userAnswer, List<QuestTaskAnswer> botAnswers) {
        for (QuestTaskAnswer botAnswer : botAnswers) {
            String answerToCheck = userAnswer;
            if (StringUtils.isEmpty(answerToCheck)) {
                continue;
            }
            if (!botAnswer.isStrict()) {
                answerToCheck = convertToNotStrict(answerToCheck);
            }
            if (botAnswer.getText() != null && botAnswer.getText().contains(answerToCheck)) {
                return botAnswer;
            }
        }
        return null;
    }

    private boolean checkIfAlreadyAnswered(QuestTaskAnswer questTaskAnswer, GameMessage gameMessage, GameTask gameTask) {
        if (questTaskAnswer != null && !questTaskAnswer.isIgnoreInCounting()) {
            boolean alreadyAnswered = gameTask.getAnswers().stream()
                    .anyMatch(a -> a != null && a.getQuestAnswerId() != null && a.getQuestAnswerId().equals(questTaskAnswer.getId()));
            if (alreadyAnswered) {
                if (!gameTask.getQuestTask().isIgnoreAlreadyAnswered()) {
                    String response = getRandomReaction(getAlreadyAnsweredReactions(gameTask));
                    eventPublisher.publishEvent(BotEvent.text(gameTask.getChatId(), response, gameMessage.getMessageId()));
                }
                return true;
            }
        }
        return false;
    }

    private String convertToNotStrict(String text) {
        return StringUtils.deleteWhitespace(text.trim().toLowerCase()).replace("ё", "е");
    }

}
