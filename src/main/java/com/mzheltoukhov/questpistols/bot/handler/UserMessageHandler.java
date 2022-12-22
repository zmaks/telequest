package com.mzheltoukhov.questpistols.bot.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mzheltoukhov.questpistols.bot.service.TelegramResourceService;
import com.mzheltoukhov.questpistols.exception.*;
import com.mzheltoukhov.questpistols.model.*;
import com.mzheltoukhov.questpistols.model.event.bot.BotEvent;
import com.mzheltoukhov.questpistols.model.event.bot.DownloadFileEvent;
import com.mzheltoukhov.questpistols.model.event.bot.ForwardedToChatBotEvent;
import com.mzheltoukhov.questpistols.model.event.bot.SendFileEvent;
import com.mzheltoukhov.questpistols.service.CompetitionService;
import com.mzheltoukhov.questpistols.service.GameService;
import com.mzheltoukhov.questpistols.service.QuestService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Component
public class UserMessageHandler {

    @Value("#{'${bot.admins:z_maks}'.split(',')}")
    private List<String> admins;

    @Value("${bot.allow-user-message:false}")
    private boolean allowUserMessages;

    private final GameService gameService;
    private final CompetitionService competitionService;
    private final TelegramResourceService resourceService;
    private final QuestService questService;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper mapper;

    @Autowired
    public UserMessageHandler(GameService gameService, CompetitionService competitionService, TelegramResourceService resourceService,
                              QuestService questService, ApplicationEventPublisher eventPublisher, ObjectMapper mapper) {
        this.gameService = gameService;
        this.competitionService = competitionService;
        this.resourceService = resourceService;
        this.questService = questService;
        this.eventPublisher = eventPublisher;
        this.mapper = mapper;
    }

    public void handle(Message message) {

//        if (message.getText() != null && message.getText().startsWith("/demo")) {
//            gameService.handleGameMessage(GameMessage.fromTelegramMessage(message));
//        } else {
//            gameService.handleGameMessage(GameMessage.fromTelegramMessage(message));
//        }

        if (message.getFrom() != null && message.getFrom().getUserName() != null && admins.contains(message.getFrom().getUserName())) {
            if (resourceService.isResourceResponse(message)) {
                resourceService.addResource(message);
                return;
            }
            if (message.hasDocument() && message.getDocument().getFileName().endsWith("json")) {
                log.info("Loading quest json: {}", message.getDocument().getFileName());
                Consumer<InputStream> fileHandler = is -> {
                    try (InputStream fileIs = is) {
                        String json = IOUtils.toString(fileIs, StandardCharsets.UTF_8);
                        questService.loadQuest(json, message.getChatId());
                    } catch (IOException e) {
                        log.error(e.getMessage(), e);
                    }
                };
                eventPublisher.publishEvent(new DownloadFileEvent(message.getDocument().getFileId(), fileHandler));
                return;
            }
            if (message.getText() != null && message.getText().startsWith("{")) {
                questService.loadQuest(message.getText(), message.getChatId());
                return;
            }
            if (message.getText() != null && message.getText().startsWith("/create")) {
                executeCreateCompetitionCommand(message);
                return;
            }
            if (message.getText() != null && message.getText().startsWith("/sendhello") && message.getText().contains(" ")) {
                executeSendStartMessageCompetitionCommand(message);
                return;
            }
            if (message.getText() != null && message.getText().startsWith("/run") && message.getText().contains(" ")) {
                executeRunCompetitionCommand(message);
                return;
            }
            if (message.getText() != null && message.getText().startsWith("/status") && message.getText().contains(" ")) {
                executeCompetitionStatusCommand(message);
                return;
            }
            if (message.getText() != null && message.getText().startsWith("/forward") && message.getText().contains(" ")) {
                executeCompetitionForwardCommand(message);
                return;
            }
            if (message.getText() != null && message.getText().startsWith("/restart") && message.getText().contains(" ")) {
                executeRestartCommand(message);
                return;
            }
            if (message.getText() != null && message.getText().startsWith("/report") && message.getText().contains(" ")) {
                executeCompetitionReportCommand(message);
                return;
            }
            if (message.getText() != null && message.getText().startsWith("/top") && message.getText().contains(" ")) {
                executeCompetitionTopCommand(message);
                return;
            }

            if (message.getText() != null && message.getText().startsWith("/stop") && message.getText().contains(" ")) {
                executeCompetitionStopCommand(message);
                return;
            }
            if (message.getText() != null && message.getText().startsWith("/next")) {
                executeNextCommand(message);
                return;
            }
            if (message.getText() != null && message.getText().startsWith("/addpoints") && message.getText().contains(" ")) {
                executeCompetitionAddPointsCommand(message);
                return;
            }

        }

        if (allowUserMessages) {
            if (message.hasText() && message.getText().startsWith("/start")) {
                executeAddGameCommand(message);
                return;
            }
            gameService.handleGameMessage(GameMessage.fromTelegramMessage(message));
        }

    }

    private void executeAddGameCommand(Message message) {

        try {
            Competition competition;
            if (message.getText() != null) {
                var parts = message.getText().split(" ");
                if (parts.length != 2) {
                    competition = competitionService.findLastCompetition();
                } else {
                    String competitionName = parts[1];
                    competition = competitionService.findByName(competitionName);
                }

            } else {
                competition = competitionService.findLastCompetition();
            }
            Game game = competitionService.addToCompetition(competition, GameMessage.fromTelegramMessage(message));
            // eventPublisher.publishEvent(BotEvent.text(message.getChatId(), "\uD83D\uDC4D"));
//            if (game.getStatus() == GameStatus.CREATED) {
//                eventPublisher.publishEvent(BotEvent.text(message.getChatId(), "Добро пожаловать! Ожидайте начала игры."));
//            }
            eventPublisher.publishEvent(BotEvent.text(competition.getCreatedById(), String.format("Чат '*%s*' добавлен в *%s*", game.getName(), competition.getName())));
        } catch (CompetitionNotFoundException e) {
            return;
            //eventPublisher.publishEvent(BotEvent.text(message.getChatId(), "Нет такой игры"));
        } catch (GameAlreadyExistsException e) {
            eventPublisher.publishEvent(BotEvent.text(message.getChatId(), "Игра уже начата :)"));
        }
    }

    private void executeCompetitionStopCommand(Message message) {
        String competitionName = getCompetitionName(message);
        if (competitionName == null) return;
        try {
            competitionService.stop(competitionName);
            eventPublisher.publishEvent(BotEvent.text(message.getChatId(), "Игра остановлена"));
        } catch (CompetitionNotFoundException e) {
            eventPublisher.publishEvent(BotEvent.text(message.getChatId(), "Игра не найдена"));
        }
    }

    private void executeCompetitionReportCommand(Message message) {
        String competitionName = getCompetitionName(message);
        String fileName = getFileName(message, "report.xls");
        if (competitionName == null) return;
        try {
            ByteArrayOutputStream stream = competitionService.generateExcelReport(competitionName);
            SendFileEvent sendFileEvent = new SendFileEvent(message.getChatId(), null, fileName, stream);
            eventPublisher.publishEvent(sendFileEvent);

        } catch (CompetitionNotFoundException e) {
            eventPublisher.publishEvent(BotEvent.text(message.getChatId(), "Игра не найдена"));
        }
    }

    private void executeCompetitionTopCommand(Message message) {
        String competitionName = getCompetitionName(message);
        String fileName = getFileName(message, "top.xls");
        if (competitionName == null) return;
        try {
            ByteArrayOutputStream stream = competitionService.topPlayers(competitionName);
            SendFileEvent sendFileEvent = new SendFileEvent(message.getChatId(), null, fileName, stream);
            eventPublisher.publishEvent(sendFileEvent);
        } catch (CompetitionNotFoundException e) {
            eventPublisher.publishEvent(BotEvent.text(message.getChatId(), "Игра не найдена"));
        }
    }

    private String getFileName(Message message, String defaultName) {
        String[] parts = message.getText().split(" ");
        String fileName = defaultName;
        if (parts.length >= 3) {
            fileName = parts[2];
            if (!fileName.endsWith(".xls")) {
                fileName += ".xls";
            }
        }
        return fileName;
    }

    private void executeRestartCommand(Message message) {
        String[] parts = message.getText().split(" ");
        if (parts.length < 3) {
            return;
        }
        String prefix = parts[1];
        String competitionName = parts[2];
        StringBuilder gameNameBuilder = new StringBuilder();
        for (int i = 3; i < parts.length; i++) {
            gameNameBuilder.append(parts[i]);
        }
        String gameName = gameNameBuilder.toString();
        if (StringUtils.isBlank(gameName)) {
            eventPublisher.publishEvent(BotEvent.text(message.getChatId(), "Нужно имя игры"));
            return;
        }
        if (competitionName == null) return;
        try {
            Competition competition = competitionService.findByName(competitionName);
            for (var game : competition.getGames()) {
                if (gameName.equalsIgnoreCase(StringUtils.deleteWhitespace(game.getName()))) {
                    try {
                        var task = gameService.restartTask(game, prefix);
                        eventPublisher.publishEvent(BotEvent.text(message.getChatId(), "Перезапустили " + task.getQuestTask().getName()));
                        return;
                    } catch (Exception e) {
                        log.error("Cannot restart task", e);
                        eventPublisher.publishEvent(BotEvent.text(message.getChatId(), "Не получилось рестартануть"));
                        return;
                    }
                }

            }
            eventPublisher.publishEvent(BotEvent.text(message.getChatId(), "Не найдена такая игра в соревновании"));
            return;
        } catch (CompetitionNotFoundException e) {
            eventPublisher.publishEvent(BotEvent.text(message.getChatId(), "Игра не найдена"));
            return;
        }

    }

    private void executeCompetitionForwardCommand(Message message) {
        if (message.getReplyToMessage() == null) {
            eventPublisher.publishEvent(BotEvent.text(message.getChatId(), "Это необходимо присылать ответом на сообщение, которое надо переслать"));
            return;
        }
        String[] parts = message.getText().split(" ");
        if (parts.length < 2) {
            return;
        }
        String competitionName = parts[1];
        StringBuilder gameNameBuilder = new StringBuilder();
        for (int i = 2; i < parts.length; i++) {
            gameNameBuilder.append(parts[i]);
        }
        String gameName = gameNameBuilder.toString();
        if (competitionName == null) return;
        try {
            Competition competition = competitionService.findByName(competitionName);
            if (StringUtils.isNotBlank(gameName)) {
                competition.getGames().stream()
                        .filter(g -> StringUtils.deleteWhitespace(g.getName()).equalsIgnoreCase(gameName))
                        .findFirst()
                        .ifPresent(g -> {
                            forwardToGame(g, message);
                            eventPublisher.publishEvent(BotEvent.text(message.getChatId(),
                                    "Сообщение успешно передано команде " + g.getName()));
                        });
            } else {
                for (var game : competition.getGames()) {
                    forwardToGame(game, message);
                    sleep(50);
                }
                eventPublisher.publishEvent(BotEvent.text(message.getChatId(), "Сообщение успешно передано командам. Количество: " + competition.getGames().size()));
            }
        } catch (CompetitionNotFoundException e) {
            eventPublisher.publishEvent(BotEvent.text(message.getChatId(), "Игра не найдена"));
            return;
        }

    }

    private void forwardToGame(Game game, Message message) {
        ForwardedToChatBotEvent event = new ForwardedToChatBotEvent();
        event.setFromChatId(message.getChatId());
        event.setForwardToChatId(game.getChatId());
        event.setForwardedMessageId(message.getReplyToMessage().getMessageId());
        eventPublisher.publishEvent(event);
    }

    private void executeCompetitionAddPointsCommand(Message message) {
        String[] parts = message.getText().split(" ");
        if (parts.length != 5) {
            return;
        }
        Integer points = Integer.parseInt(parts[1]);
        Integer taskNumber = Integer.parseInt(parts[2]);
        String competitionName = parts[3];
        String gameName = parts[4];
        if (competitionName == null) return;
        try {
            competitionService.addPoints(competitionName, gameName, taskNumber, points);
            eventPublisher.publishEvent(BotEvent.text(message.getChatId(), "Очки добавлены"));
        } catch (CompetitionNotFoundException e) {
            eventPublisher.publishEvent(BotEvent.text(message.getChatId(), "Игра не найдена"));
            return;
        } catch (IllegalArgumentException e) {
            eventPublisher.publishEvent(BotEvent.text(message.getChatId(), e.getMessage()));
            return;
        }


    }

    private void executeCompetitionStatusCommand(Message message) {
        String competitionName = getCompetitionName(message);
        if (competitionName == null) return;
        try {
            final int max = 30;
            List<String> statusList = competitionService.status(competitionName);
            for (int i = 0; i < statusList.size() / max + 1; i++) {
                int last = (i+1) * max;
                if (last > statusList.size()) {
                    last = statusList.size();
                }
                List<String> part = statusList.subList(i * max, last);
                String status = String.join("\n", part);
                eventPublisher.publishEvent(BotEvent.text(message.getChatId(), status));
                sleep(100);
            }
        } catch (CompetitionNotFoundException e) {
            eventPublisher.publishEvent(BotEvent.text(message.getChatId(), "Игра не найдена"));
        }
    }

    private void executeSendStartMessageCompetitionCommand(Message message) {
        String competitionName = getCompetitionName(message);
        if (competitionName == null) return;
        try {
            competitionService.sendStartMessage(competitionName);
            eventPublisher.publishEvent(BotEvent.text(message.getChatId(), "Приветственное сообщение успешно отправлено"));
        } catch (CompetitionNotFoundException e) {
            eventPublisher.publishEvent(BotEvent.text(message.getChatId(), "Игра не найдена"));
        } catch (CompetitionAlreadyStartedException e) {
            eventPublisher.publishEvent(BotEvent.text(message.getChatId(), "Игра уже запущена"));
        }
    }

    private void executeNextCommand(Message message) {
        String[] parts = message.getText().split(" ");
        String competitionName = null;
        if (parts.length > 1) {
            competitionName = parts[1];
        }
        try {
            competitionService.nextTask(competitionName);
            eventPublisher.publishEvent(BotEvent.text(message.getChatId(), "Отправлено следующее задание"));
        } catch (CompetitionNotFoundException e) {
            eventPublisher.publishEvent(BotEvent.text(message.getChatId(), "Игра не найдена"));
        }
    }

    private void executeRunCompetitionCommand(Message message) {
        String competitionName = getCompetitionName(message);
        if (competitionName == null) return;
        try {
            Competition competition = competitionService.start(competitionName);
            String startedGames = competition.getGames().stream().map(Game::getName).collect(Collectors.joining("\n"));
            eventPublisher.publishEvent(BotEvent.text(message.getChatId(), "*Игра успешно запущена в чатах:*\n" + startedGames));
        } catch (CompetitionNotFoundException e) {
            eventPublisher.publishEvent(BotEvent.text(message.getChatId(), "Игра не найдена"));
        } catch (CompetitionAlreadyStartedException e) {
            eventPublisher.publishEvent(BotEvent.text(message.getChatId(), "Игра уже запущена"));
        }

    }

    private boolean executeCreateCompetitionCommand(Message message) {
        String[] parts = message.getText().split(" ");
        String competitionName = null;
        if (parts.length > 1) {
            competitionName = parts[1];
        } else {
            eventPublisher.publishEvent(BotEvent.text(message.getChatId(), "Ошибка. Необходимо указать имя (без пробелов)"));
            return true;
        }
        Integer players = null;
        if (parts.length > 2) {
            if (StringUtils.isNumeric(parts[2])) {
                players = Integer.parseInt(parts[2]);
            } else {
                eventPublisher.publishEvent(BotEvent.text(message.getChatId(), "Ошибка. Количетво участников указано неверно или название содержит пробел"));
                return true;
            }
        }

        String codePhrase = null;
        if (parts.length > 3) {
            codePhrase = parts[3];
        }

        Integer batchSize = 5;
        if (parts.length > 4) {
            batchSize = Integer.parseInt(parts[4]);
        }
        try {
            competitionService.create(competitionName, players, message.getChatId(), codePhrase, batchSize);
        } catch (CompetitionExistsException e) {
            eventPublisher.publishEvent(BotEvent.text(message.getChatId(), "Ошибка. Игра с тактим именем уже есть"));
            return true;
        } catch (DefaultQuestNotFoundException e) {
            eventPublisher.publishEvent(BotEvent.text(message.getChatId(), "Ошибка. Не найден подходяший квест"));
            return true;
        }
        eventPublisher.publishEvent(BotEvent.text(message.getChatId(),
                String.format("Игра успешно создана!\nНазвание: %s\nМакс. кол-во участников в одном чате: %s\nКодовая фраза: %s\nРаспределение регистрации: %s",
                        competitionName,
                        players == null ? "неограничено." : players,
                        codePhrase == null ? "не установлена." : codePhrase,
                        batchSize)));
        return false;
    }

    public void handleCallback(CallbackQuery callbackQuery) {
        GameMessage gameMessage = GameMessage.fromTelegramCallback(callbackQuery);
        gameService.handleGameMessage(gameMessage);
    }

    private String getCompetitionName(Message message) {
        String[] parts = message.getText().split(" ");
        if (parts.length < 2) {
            eventPublisher.publishEvent(BotEvent.text(message.getChatId(), "После команды необходимо указать название игры без пробелов.\nПример: /command game-12"));
            return null;
        }
        return parts[1];
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }
}
