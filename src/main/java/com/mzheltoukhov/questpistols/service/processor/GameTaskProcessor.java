package com.mzheltoukhov.questpistols.service.processor;

import com.google.common.collect.Lists;
import com.mzheltoukhov.questpistols.model.*;
import com.mzheltoukhov.questpistols.model.event.GameTaskEvent;
import com.mzheltoukhov.questpistols.model.event.bot.BotEvent;
import com.mzheltoukhov.questpistols.model.event.bot.BotEventButton;
import com.mzheltoukhov.questpistols.model.event.bot.BotEventPayload;
import com.mzheltoukhov.questpistols.model.task.QuestTaskAnswer;
import com.mzheltoukhov.questpistols.model.task.QuestTaskPlug;
import com.mzheltoukhov.questpistols.model.task.ResponsePayload;
import com.mzheltoukhov.questpistols.model.task.TaskHint;
import com.mzheltoukhov.questpistols.repository.GameRepository;
import com.mzheltoukhov.questpistols.repository.GameTaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.mzheltoukhov.questpistols.model.GameTaskStatus.*;

@Slf4j
public abstract class GameTaskProcessor implements TaskProcessor {

    public static final String PLUG_ANSWER_DATA = "plug-ready";

    private final Map<String, Function<GameTask, String>> wildcardFunctions = Map.of(
            "{{remained-answers}}", this::getRemainedAnswersFromTask,
            "{{answer-user}}", this::getAnswerUserFromTask,
            "{{last-answer}}", this::getLastAnswerFromTask
    );

    @Autowired
    protected ApplicationEventPublisher eventPublisher;
    @Autowired
    protected GameTaskRepository gameTaskRepository;
    @Autowired
    protected GameRepository gameRepository;

    @Value("${quest.command.start-sign:/}")
    protected String startSymbol;

    @Value("${bot.allow-user-message:false}")
    private boolean allowUserMessages;

    public void processTask(GameTask gameTask, GameMessage gameMessage) {
        if (gameTask.getStatus().equals(FINISHED)) {
            return;
        }

        if (gameMessage != null) {
            if (gameMessage.getCallBackAnswer() != null) {
                log.info("{} - {} ({})| Received callback: {}", gameMessage.getChatName(), gameMessage.getChatId(), gameMessage.getUsername(), gameMessage.getCallBackAnswer());
            } else {
                log.info("{} - {} ({})| Received message: {}", gameMessage.getChatName(), gameMessage.getChatId(), gameMessage.getUsername(), gameMessage.getText());
            }
        }

        if (gameTask.isWait()) {
            log.info("{} -- {} - {}: Is waiting", gameTask.getChatId(), gameTask.getQuestTask().getQuestName(), gameTask.getQuestTask().getName());
            return;
        }
        if (gameTask.getStatus().equals(CREATED)) {
            runTask(gameTask, gameMessage);
            log.info("{}| STARTED task: {}", gameTask.getChatId(), gameTask.getQuestTask().getName());
            return;
        }
        if (gameTask.getStatus().equals(STARTED)) {
            if (checkHint(gameTask, gameMessage)) {
                return;
            }
            doSynchronously(task -> processStartedTask(task, gameMessage), gameTask);
        }
    }

    private boolean checkHint(GameTask gameTask, GameMessage gameMessage) {
        if (gameTask.getHintTime() != null && !gameTask.isHintSent() && OffsetDateTime.now().isAfter(gameTask.getHintTime()) && gameMessage != null && gameMessage.getText() != null) {
            String key = StringUtils.deleteWhitespace(parseAnswer(gameMessage));
            if (!getHintKeyWord(gameTask).equalsIgnoreCase(key)) {
                return false;
            }
            log.info("{}| Hint time: {}", gameTask.getChatId(), gameTask.getQuest().getName());
            sendDescriptions(gameTask.getQuestTask().getHint().getDescriptions(), gameTask);
            Integer fine = getHintFinePoints(gameTask);
            if (fine != null) {
                gameTask.setPoints(gameTask.getPoints() - fine);
            }
            gameTask.setHintSent(true);
            gameTaskRepository.save(gameTask);
            return true;
        }
        return false;
    }

    protected abstract void processStartedTask(GameTask gameTask, GameMessage gameMessage);

    public void updateState(GameTask gameTask) {
        if (gameTask.getEndTime() != null && OffsetDateTime.now().isAfter(gameTask.getEndTime())) {
            doSynchronously(task -> {
                if (task.getStatus() == FINISHED) {
                    log.info("{}| {} task already finished", gameTask.getChatId(), gameTask.getQuestTask().getName());
                    return;
                }
                log.info("{}| Task timeout: {}", gameTask.getChatId(), gameTask.getQuest().getName());
                gameTask.setFinishedByTimeout(true);
                finishTask(gameTask, BotEvent.text(gameTask.getChatId(), getRandomReaction(getTimeoutReactions(gameTask))));
                eventPublisher.publishEvent(GameTaskEvent.finished(gameTask.getId()));
            }, gameTask);
        }
        if (gameTask.getHintTime() != null && !gameTask.isHintAnnounceSent() && OffsetDateTime.now().isAfter(gameTask.getHintTime())) {
            log.info("{}| Hint announce time: {}", gameTask.getChatId(), gameTask.getQuest().getName());
            eventPublisher.publishEvent(BotEvent.text(gameTask.getChatId(), getHintAnnounceText(gameTask)));
            gameTask.setHintAnnounceSent(true);
            gameTaskRepository.save(gameTask);
        }
    }

    protected void doSynchronously(Consumer<GameTask> consumer, GameTask gameTask) {
        try {
            int retries = 0;
            GameTask lockedTask = null;
            while (lockedTask == null && retries < 15) {
                lockedTask = gameTaskRepository.lockGameTask(gameTask.getId());
                if (lockedTask == null) {
                    log.info("{}| {} task is locked. Sleep 200ms", gameTask.getChatId(), gameTask.getQuestTask().getName());
                    Thread.sleep(200);
                    retries++;
                }
            }
            if (lockedTask == null) {
                lockedTask = gameTask;
            }
            consumer.accept(lockedTask);
        }catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error(e.getMessage(), e);
        } finally {
            gameTaskRepository.unlockGameTask(gameTask.getId());
        }
    }

    protected void finishTask(GameTask gameTask, BotEvent botEvent) {
        gameTask.setStatus(GameTaskStatus.FINISHED);
        gameTaskRepository.save(gameTask);
        if (gameTask.isFinishedByTimeout()) {
            Game game = gameRepository.findById(gameTask.getGameId()).get();
            game.setMinPlayersNotificationSent(false);
            gameRepository.save(game);
        }
        if (botEvent != null) {
            eventPublisher.publishEvent(botEvent);
        }
        if (getFinalTaskReaction(gameTask) != null && !gameTask.isFinishedByTimeout()) {
            eventPublisher.publishEvent(BotEvent.text(gameTask.getChatId(), getFinalTaskReaction(gameTask)));
        }
        if (getFinalTaskReactionPayload(gameTask) != null && !gameTask.isFinishedByTimeout()) {
            for (var payload : getFinalTaskReactionPayload(gameTask)) {
                sendPayload(gameTask.getChatId(), payload, gameTask);
                sleep(100);
            }
        }
    }

    protected void runTask(GameTask gameTask, GameMessage gameMessage) {
        QuestTaskPlug plug = gameTask.getQuestTask().getPlug();
        if (plug != null) {
            if (!gameTask.isPlugSent()) {
                if (plug.getPayload() != null) {
                    sendPayload(gameTask.getChatId(), plug.getPayload(), gameTask);
                }
                if (plug.getText() != null) {
                    BotEvent event = BotEvent.text(gameTask.getChatId(), plug.getText());
                    event.setButton(BotEventButton.builder().name(plug.getButton()).data(PLUG_ANSWER_DATA).build());
                    eventPublisher.publishEvent(event);
                } else if (plug.getPayloads() != null) {
                    for (int i = 0; i < plug.getPayloads().size(); i++) {
                        if (i < plug.getPayloads().size() - 1) {
                            sendPayload(gameTask.getChatId(), plug.getPayloads().get(i), gameTask);
                            sleep(100);
                        } else {
                            BotEvent event = BotEvent.builder()
                                    .chatId(gameTask.getChatId())
                                    .message(plug.getPayloads().get(i).getText())
                                    .payloadType(plug.getPayloads().get(i).getType())
                                    .resourceName(plug.getPayloads().get(i).getResourceName())
                                    .build();
                            event.setButton(BotEventButton.builder().name(plug.getButton()).data(PLUG_ANSWER_DATA).build());
                            eventPublisher.publishEvent(event);
                        }
                    }
                }

                gameTask.setPlugSent(true);
                gameTaskRepository.save(gameTask);
                return;
            }
            if (gameMessage == null) {
                return;
            }
            if (gameMessage.getCallBackAnswer() == null || !gameMessage.getCallBackAnswer().equalsIgnoreCase(PLUG_ANSWER_DATA)) {
                log.info("{}|{} Incorrect plug answer: {}.", gameMessage.getChatName(), gameTask.getQuestTask().getName(), gameMessage.getCallBackAnswer());
                return;
            }
            if (!allowUserMessages) {
                eventPublisher.publishEvent(BotEvent.text(gameTask.getChatId(), "Игрок *" + gameMessage.getUserTitle() + "* запускает задание!"));
            }
        }
        gameTask.setStartTime(OffsetDateTime.now());
        TaskHint taskHint = gameTask.getQuestTask().getHint();
        if (taskHint != null) {
            Integer time = getHintTime(gameTask);
            if (time > 0) {
                gameTask.setHintTime(gameTask.getStartTime().plusSeconds(time));
            }
        }
        if (gameTask.getQuestTask().isChoosePlayerToAnswer() && gameTask.getPlayerToAnswer() == null) {
            Game game = gameRepository.findById(gameTask.getGameId()).get();
            if (!CollectionUtils.isEmpty(game.getPlayers())) {
                Player player = game.getPlayers().stream()
                        .min(Comparator.comparing(Player::getLastAnswer, Comparator.nullsFirst(Comparator.naturalOrder()))).get();
                gameTask.setPlayerToAnswer(player);
            }
        }
        gameTask.setStatus(STARTED);
        Integer timeout = getTimeoutSeconds(gameTask);
        if (timeout != null && timeout > 0) {
            gameTask.setEndTime(OffsetDateTime.now().plusSeconds(timeout));
        }
        gameTaskRepository.save(gameTask);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
        sendDescriptions(gameTask.getQuestTask().getDescriptions(), gameTask, gameTask.getQuestTask().isPinFirst());
        if (gameTask.getPlayerToAnswer() != null) {
            eventPublisher.publishEvent(
                    BotEvent.text(gameTask.getChatId(), "На этот вопрос отвечает " + gameTask.getPlayerToAnswer().getMention() + "!"));
        }
    }

    private void sendDescriptions(List<ResponsePayload> descriptions, GameTask gameTask, boolean pinFirst) {
        if (descriptions.size() == 1) {
            sendPayload(gameTask.getChatId(), descriptions.get(0), pinFirst, gameTask);
            return;
        }

        List<BotEventPayload> groupMedias = new ArrayList<>();
        for (int i = 0; i < descriptions.size(); i++) {
            ResponsePayload payload = descriptions.get(i);
            // photos, videos and documents are sent in a group
            if (canBeGrouped(payload)) {
                // if next payload also should be added to the group then just add the payload to list
                if (i + 1 < descriptions.size()
                        && canBeGrouped(descriptions.get(i + 1))) {
                    groupMedias.add(new BotEventPayload(payload.getType(), null, payload.getResourceName()));
                } else {
                    // if there are some payloads from previous steps then add the current one to the group and send out
                    if (groupMedias.size() > 0) {
                        groupMedias.add(new BotEventPayload(payload.getType(), null, payload.getResourceName()));
                        sendGroupedPayload(gameTask.getChatId(), groupMedias);
                        groupMedias = new ArrayList<>();
                    } else {
                        // this is a single payload so should be sent without any group
                        sendPayload(gameTask.getChatId(), payload, pinFirst && i == 0, gameTask);
                    }
                }
            } else {
                sendPayload(gameTask.getChatId(), payload, pinFirst && i == 0, gameTask);
            }

        }
    }
    private void sendDescriptions(List<ResponsePayload> descriptions, GameTask gameTask) {
        sendDescriptions(descriptions, gameTask, true);
    }

    private boolean canBeGrouped(ResponsePayload payload) {
        return (payload.getType() == PayloadType.PHOTO && payload.getText() == null)
                || payload.getType() == PayloadType.VIDEO || payload.getType() == PayloadType.DOCUMENT;
    }

    protected void sendPayload(Long chatId, ResponsePayload responsePayload, GameTask gameTask) {
        sendPayload(chatId, responsePayload, false, gameTask);
    }

    protected void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    protected void sendPayload(Long chatId, ResponsePayload responsePayload, boolean pin, GameTask gameTask) {
        sleep(100);
        String text = responsePayload.getText();
        text = applyWildcards(text, gameTask);
        BotEvent botEvent = BotEvent.builder()
                .chatId(chatId)
                .message(text)
                .payloadType(responsePayload.getType())
                .resourceName(responsePayload.getResourceName())
                .pin(pin)
                .build();
        if (!CollectionUtils.isEmpty(responsePayload.getButtons())) {
            botEvent.setButtons(
                    responsePayload.getButtons().stream()
                            .map(b -> BotEventButton.builder().name(b.getLabel()).data(gameTask.getId() + ":" + b.getAnswer()).build())
                            .collect(Collectors.toList())
            );
        }
        eventPublisher.publishEvent(botEvent);
    }

    protected void sendGroupedPayload(Long chatId, List<BotEventPayload> payloads) {
        final double maxMediaInGroup = 10f;
        double partitions =  Math.ceil(payloads.size() /  maxMediaInGroup);
        int partitionSize = (int) Math.ceil(payloads.size() / partitions);
        for (List<BotEventPayload> mediaGroup : Lists.partition(payloads, partitionSize)) {
            BotEvent botEvent = BotEvent.builder()
                    .chatId(chatId)
                    .payloadGroup(mediaGroup)
                    .build();
            eventPublisher.publishEvent(botEvent);
        }
    }

    protected String parseAnswer(GameMessage gameMessage) {
        if (gameMessage.getText() == null) return null;
        String answer = gameMessage.getText().trim();
        if (answer.startsWith(startSymbol)) {
            answer = answer.substring(1);
        }
        return answer;
    }

    protected String checkAndParserCallbackAnswer(GameMessage gameMessage, GameTask gameTask) {
        if (gameMessage.getCallBackAnswer() == null) return null;
        String[] dataParts = gameMessage.getCallBackAnswer().split(":");
        if (dataParts.length != 2) {
            log.warn("Incorrect callback answer format: {}", gameMessage.getCallBackAnswer());
            return null;
        }
        String gameTaskId = dataParts[0];
        String answer = dataParts[1];
        if (!gameTaskId.equals(gameTask.getId())) {
            log.info("Incorrect callback answer (wrong game task). Current task: {}, callback: {}, user: {}", gameTask.getQuestTask().getName(), gameMessage.getCallBackAnswer(), gameMessage.getUserTitle());
            return null;
        }
        return answer;
    }

    protected void negativeReaction(GameTask gameTask, GameMessage gameMessage) {
        if (gameTask.getQuestTask().isNeutralReaction()) {
            eventPublisher.publishEvent(BotEvent.text(gameTask.getChatId(), "Ответ принят!", gameMessage.getMessageId()));
            return;
        }
        List<String> negativeReactions = getNegativeReactions(gameTask);
        eventPublisher.publishEvent(BotEvent.text(gameTask.getChatId(), getRandomReaction(negativeReactions), gameMessage.getMessageId()));
    }

    protected void positiveReaction(GameTask gameTask, QuestTaskAnswer questTaskAnswer, GameMessage gameMessage) {
        if (gameTask.getQuestTask().isNeutralReaction()) {
            eventPublisher.publishEvent(BotEvent.text(gameTask.getChatId(), "Ответ принят!", gameMessage.getMessageId()));
            return;
        }
        if (questTaskAnswer != null && !CollectionUtils.isEmpty(questTaskAnswer.getReactions())) {
            sendDescriptions(questTaskAnswer.getReactions(), gameTask, false);
        } else {
            String reaction = getRandomReaction(getPositiveReactions(gameTask));
            reaction = applyWildcards(reaction, gameTask);
            eventPublisher.publishEvent(BotEvent.text(gameTask.getChatId(), reaction, gameMessage.getMessageId()));
        }
    }

    protected void positiveReaction(GameTask gameTask, GameMessage gameMessage) {
        positiveReaction(gameTask, null, gameMessage);
    }

    private String applyWildcards(String text, GameTask gameTask) {
        if (StringUtils.isNotBlank(text) && wildcardFunctions.keySet().stream().anyMatch(text::contains)) {
            for (var entry : wildcardFunctions.entrySet()) {
                String result = entry.getValue().apply(gameTask);
                int remained = -1;
                try {
                    remained = Integer.parseInt(result);
                } catch (NumberFormatException ignore) {

                }
                if (entry.getKey().equals("{{remained-answers}}") && remained <= 0) {
                    text = text.substring(0, text.indexOf("\n") + 1);
                    continue;
                }
                text = text.replace(entry.getKey(), result);
            }
        }
        return text;
    }

    protected <T> T getRandomReaction(List<T> reactions) {
        return reactions.get(new Random().nextInt(reactions.size()));
    }

    public Integer getHintTime(GameTask gameTask) {
        if (gameTask.getQuestTask().getHint().getHintTimeInSeconds() != null) {
            return gameTask.getQuestTask().getHint().getHintTimeInSeconds();
        }
        return getBaseParamsField(gameTask, QuestBaseParams::getHintTimeInSeconds, getTimeoutSeconds(gameTask) / 2);
    }

    public Integer getHintFinePoints(GameTask gameTask) {
        if (gameTask.getQuestTask().getHint().getHintFinePoints() != null) {
            return gameTask.getQuestTask().getHint().getHintFinePoints();
        }
        return getBaseParamsField(gameTask, QuestBaseParams::getHintFinePoints, 0);
    }

    public String getHintAnnounceText(GameTask gameTask) {
        if (gameTask.getQuestTask().getHint().getHintAnnounceText() != null) {
            return gameTask.getQuestTask().getHint().getHintAnnounceText();
        }
        return getBaseParamsField(gameTask, QuestBaseParams::getHintAnnounceText, null);
    }

    public String getHintKeyWord(GameTask gameTask) {
        if (gameTask.getQuestTask().getHint().getHintKeyWord() != null) {
            return gameTask.getQuestTask().getHint().getHintKeyWord();
        }
        return getBaseParamsField(gameTask, QuestBaseParams::getHintKeyWord, null);
    }

    public Integer getPoints(GameTask gameTask, QuestTaskAnswer questTaskAnswer, OffsetDateTime now) {
        Integer points = null;
        Integer finePoints = null;
        Integer pointsTime = null;
        if (questTaskAnswer != null) {
            points = questTaskAnswer.getPoints();
            pointsTime = questTaskAnswer.getPointsTime();
            finePoints = questTaskAnswer.getFinePoints();
        }
        if (points == null) {
            points = getBaseParamsField(gameTask, QuestBaseParams::getPoints, 0);
        }
        if (finePoints == null) {
            finePoints = getBaseParamsField(gameTask, QuestBaseParams::getFinePoints, null);
        }
        if (pointsTime == null) {
            pointsTime = getBaseParamsField(gameTask, QuestBaseParams::getPointsTimeSeconds, null);
        }

        if (finePoints == null || pointsTime == null) {
            return points;
        }
        int timeSpent = (int) (now.toEpochSecond() - gameTask.getStartTime().toEpochSecond());
        if (timeSpent == 0) {
            return points;
        }
        double cf = (double) timeSpent / (double) pointsTime;
        if (cf > 1) cf = 1;
        points -= BigDecimal.valueOf(cf).multiply(BigDecimal.valueOf((double) finePoints)).setScale(0, RoundingMode.HALF_UP).intValue();

        return points;
    }

    public <T> T getBaseParamsField(GameTask gameTask, Function<QuestBaseParams, T> function, T defaultVal) {
        if (gameTask.getQuestTask() != null && gameTask.getQuestTask().getBaseParams() != null
                && function.apply(gameTask.getQuestTask().getBaseParams()) != null) {
            return function.apply(gameTask.getQuestTask().getBaseParams());
        }
        if (gameTask.getQuest() != null && gameTask.getQuest().getBaseParams() != null
                && function.apply(gameTask.getQuest().getBaseParams()) != null) {
            return function.apply(gameTask.getQuest().getBaseParams());
        }
        if (QuestBaseParams.defaults() != null && function.apply(QuestBaseParams.defaults()) != null) {
            return function.apply(QuestBaseParams.defaults());
        }
        return defaultVal;
    }

    private String getRemainedAnswersFromTask(GameTask gameTask) {
        gameTask = gameTaskRepository.findById(gameTask.getId()).get();
        if (gameTask.getQuestTask().getRequiredCorrectAnswers() == null) {
            return "?";
        }
        return (gameTask.getQuestTask().getRequiredCorrectAnswers() - gameTask.getAnswers().stream().filter(a -> !a.isBonus()).count()) + "";
    }

    private String getLastAnswerFromTask(GameTask gameTask) {
        if (CollectionUtils.isEmpty(gameTask.getAnswers())) {
            return "?";
        }
        return gameTask.getAnswers().get(gameTask.getAnswers().size()-1).getAnswer();
    }

    private String getAnswerUserFromTask(GameTask gameTask) {
        if (CollectionUtils.isEmpty(gameTask.getAnswers())) {
            return "?";
        }
        return gameTask.getAnswers().get(gameTask.getAnswers().size()-1).getPlayer();
    }

    protected Integer getTimeoutSeconds(GameTask gameTask) {
        return getBaseParamsField(gameTask, QuestBaseParams::getTimeoutSeconds, 0);
    }

    protected List<String> getPositiveReactions(GameTask gameTask) {
        return getBaseParamsField(gameTask, QuestBaseParams::getPositiveReactions, Collections.emptyList());
    }

    protected List<String> getNegativeReactions(GameTask gameTask) {
        return getBaseParamsField(gameTask, QuestBaseParams::getNegativeReactions, Collections.emptyList());
    }

    protected List<String> getTimeoutReactions(GameTask gameTask) {
        return getBaseParamsField(gameTask, QuestBaseParams::getTimeoutReactions, Collections.emptyList());
    }

    protected List<String> getAlreadyAnsweredReactions(GameTask gameTask) {
        return getBaseParamsField(gameTask, QuestBaseParams::getAlreadyAnsweredReactions, Collections.emptyList());
    }

    protected String getAttemptsExhaustedReaction(GameTask gameTask) {
        return getBaseParamsField(gameTask, QuestBaseParams::getAttemptsExhaustedReaction, null);
    }

    public String getFinalTaskReaction(GameTask gameTask) {
        return getBaseParamsField(gameTask, QuestBaseParams::getFinalTaskReaction, null);
    }

    public List<ResponsePayload> getFinalTaskReactionPayload(GameTask gameTask) {
        return getBaseParamsField(gameTask, QuestBaseParams::getFinalTaskReactionPayload, null);
    }

    protected boolean isTaskFinished(GameTask gameTask) {
        return gameTask.getAnswers().stream().filter(a -> !a.isBonus()).count() >= gameTask.getQuestTask().getRequiredCorrectAnswers();
    }

    protected List<QuestTaskAnswer> getNotAnswered(GameTask gameTask) {
        return gameTask.getQuestTask().getAnswers().stream()
                .filter(ans -> gameTask.getAnswers().stream().noneMatch(gameAns -> gameAns.getQuestAnswerId().equals(ans.getId())))
                .collect(Collectors.toList());
    }

    protected String getPlayerName(GameMessage gameMessage) {
        if (gameMessage == null) {
            return "unknown";
        }
        if (gameMessage.getPlayer() == null) {
            return gameMessage.getUserTitle();
        }
        String name = gameMessage.getPlayer().getName();
        if (gameMessage.getPlayer().getUsername() != null) {
            name = name + " (" + gameMessage.getPlayer().getUsername() + ")";
        }
        return name;
    }

}
