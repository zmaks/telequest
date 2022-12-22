package com.mzheltoukhov.questpistols.service;

import com.mzheltoukhov.questpistols.exception.CompetitionPlayersLimitException;
import com.mzheltoukhov.questpistols.exception.GameAlreadyExistsException;
import com.mzheltoukhov.questpistols.exception.GameNotFoundException;
import com.mzheltoukhov.questpistols.exception.GamePlayersLimitException;
import com.mzheltoukhov.questpistols.model.*;
import com.mzheltoukhov.questpistols.model.event.GameTaskEvent;
import com.mzheltoukhov.questpistols.model.event.bot.BotEvent;
import com.mzheltoukhov.questpistols.model.task.QuestTask;
import com.mzheltoukhov.questpistols.repository.GameRepository;
import com.mzheltoukhov.questpistols.repository.GameTaskRepository;
import com.mzheltoukhov.questpistols.service.processor.TaskProcessor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GameService {

    private final ApplicationEventPublisher eventPublisher;
    private final QuestService questService;
    private final GameRepository gameRepository;
    private final TaskProcessor gameTaskProcessor;
    private final GameTaskRepository gameTaskRepository;
    @Value("${quest.command.start-sign:/}")
    protected String startSymbol;
    @Value("${quest.start.enabled:true}")
    protected Boolean startEnabled;
    @Value("#{'${bot.admins:z_maks}'.split(',')}")
    private List<String> admins;
    @Value("${game.system-players:1}")
    protected Integer systemPlayersCount;
    private Set<String> gameCodes = new HashSet<>();

    @Value("${game.system-min-players:0}")
    protected Integer systemMinPlayersCount;

    @Autowired
    public GameService(ApplicationEventPublisher eventPublisher, GameRepository gameRepository,
                       QuestService questService, TaskProcessor gameTaskProcessor,
                       GameTaskRepository gameTaskRepository) {
        this.eventPublisher = eventPublisher;
        this.gameRepository = gameRepository;
        this.questService = questService;
        this.gameTaskProcessor = gameTaskProcessor;
        this.gameTaskRepository = gameTaskRepository;
    }

    @PostConstruct
    public void unlockAll() {
        List<GameTask> gameTasks = gameTaskRepository.findAll();
        for (var task : gameTasks) {
            task.setLock(false);
        }
        gameTaskRepository.saveAll(gameTasks);
        gameRepository.findAll().stream()
                .map(Game::getRegistrationCode)
                .filter(Objects::nonNull)
                .forEach(gameCodes::add);
    }

    @Scheduled(fixedRate = 10000)
    public void checkGamesToFinish() {
        List<String> gamesToFinishIds = gameRepository.findGamesToFinishIds();
        List<Game> gamesToFinish = new ArrayList<>();
        if (!gamesToFinishIds.isEmpty()) {
            log.info("Finishing GAMES({}): {}", gamesToFinishIds.size(), gamesToFinishIds);

            for (String gameId : gamesToFinishIds) {
                Game game = gameRepository.findById(gameId).get();
                gamesToFinish.add(game);
                finishGame(game);
            }
            for (Game game : gamesToFinish) {
                sendFinishGameNotifications(game);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    public void handleGameMessage(GameMessage gameMessage) {
        //TODO ???
        if ((gameMessage.getText() != null && gameMessage.getPayload() == null && !gameMessage.getText().startsWith(startSymbol) && !gameMessage.getText().startsWith("/")) ) {
            return;
        }
        Game startedGame = gameRepository.findByChatIdAndStatus(gameMessage.getChatId(), GameStatus.STARTED);
        if (startedGame == null) {
            startGame(gameMessage);
            return;
        }
        processGameTask(startedGame, gameMessage);
    }

    @EventListener
    public void onGameTaskEvent(GameTaskEvent event) {
        if (event.isTaskFinished()) {
            GameTask gameTask = gameTaskRepository.findById(event.getGameTaskId()).orElseThrow(RuntimeException::new);
            Game game = gameRepository.findById(gameTask.getGameId()).orElse(null);
            int points = game.getTasks().stream().mapToInt(GameTask::getPoints).sum();
            game.setPoints(points);
            setNextTask(game);
            processGameTask(game, null);
        }
    }

//    @Scheduled(fixedRateString = "${chat-members-check-interval:300000}")
//    public void checkChats() throws ExecutionException, InterruptedException {
//        CompletableFuture.runAsync(() -> {
//
//        }).get();
//    }

    public GameTask restartTask(Game game, String prefix) {
        int index = -1;
        Quest quest = questService.findByName(game.getQuestName());
        for (int i = 0; i < quest.getTasks().size(); i++) {
            if (quest.getTasks().get(i).getName().startsWith(prefix)) {
                index = i;
            }
        }
        if (index == -1) {
            throw new RuntimeException("Task not found by the prefix " + prefix);
        }
        GameTask gameTask = game.getCurrentTask();
        if (gameTask == null) {
            throw new RuntimeException("No current task");
        }
        if (index > gameTask.getIndex()) {
            return gameTask;
        }
        for (int i = gameTask.getIndex(); i >= index; i--) {
            GameTask currTask = resetGameTask(game.getTasks().get(i));
            if (i == index) {
                gameTask = currTask;
                game.setCurrentTask(currTask);
            }
        }
//        gameTask = resetGameTask(gameTask);
//        game.setCurrentTask(gameTask);
        game = gameRepository.save(game);
        processGameTask(game, null);
        return gameTask;
    }

    private GameTask resetGameTask(GameTask gameTask) {
        gameTask.setPoints(0);
        gameTask.setAnswers(new ArrayList<>());
        gameTask.setAttempts(0);
        gameTask.setEndTime(null);
        gameTask.setStartTime(null);
        gameTask.setHintTime(null);
        gameTask.setStatus(GameTaskStatus.CREATED);
        gameTask.setHintSent(false);
        gameTask.setHintAnnounceSent(false);
        gameTask.setPlugSent(false);
        gameTask.setLock(false);
        gameTask.setPlayerToAnswer(null);
        gameTask.setPlayerToAnswerRemindSent(false);
        gameTask.setFinishedByTimeout(false);
        gameTask = gameTaskRepository.save(gameTask);
        return gameTask;
    }

    @Scheduled(fixedRateString = "${game.task.started.check-period-ms:1000}")
    public void handleStartedTasks() {
        gameTaskRepository.findByStatus(GameTaskStatus.STARTED)
                .parallelStream()
                .forEach(gameTaskProcessor::updateState);
    }

    private void startGame(GameMessage gameMessage) {
        if (gameMessage.getText() == null) {
            return;
        }
        Quest quest = null;
        if (gameMessage.getText().startsWith("/start") && startEnabled) {
            quest = questService.getDemoQuest();

        } else if (gameMessage.getText().startsWith(startSymbol + "старт ")) {
            log.info("{} - {} ({})| Attempt to start game: {}", gameMessage.getChatName(), gameMessage.getChatId(), gameMessage.getUsername(), gameMessage.getText());
            String[] parts = gameMessage.getText().split(" ");
            quest = questService.findByKeyWord(parts[1]);
            //eventPublisher.publishEvent(BotEvent.text(gameMessage.getChatId(), "У меня нет для вас запланированных квестов :(\nОбратитесь к админу."));
        }
        if (quest == null) {
            log.warn("Quests not found for message {}: {}", gameMessage.getChatName(), gameMessage.getText());
            return;
        }

        Game game = initGameFromQuest(quest, gameMessage, "default");
        game = initGameTasks(game);
        game.setStatus(GameStatus.STARTED);
        game.setStartTime(OffsetDateTime.now());
        gameRepository.save(game);
        log.info("{} - {}| Start game {}", gameMessage.getChatName(), gameMessage.getChatId(), game.getName());

        processGameTask(game, gameMessage);
    }

    public Game create(GameMessage gameMessage, Integer maxPlayers, String competitionName, String questName) throws GameAlreadyExistsException {
        if (gameRepository.existsByChatIdAndCompetition(gameMessage.getChatId(), competitionName)) {
            throw new GameAlreadyExistsException("Game for such chat and competition already exists");
        }
        Quest quest = questService.findByName(questName);
        Game game = initGameFromQuest(quest, gameMessage, competitionName);
        game.setMaxPlayers(maxPlayers);
        game.setStatus(GameStatus.CREATED);
        game.setRegistrationCode(generateRegistrationCode());
        return gameRepository.save(game);
    }

    private String generateRegistrationCode() {
        String code;
        do {
            int num = new Random().nextInt(9999);
            code = String.format("%04d", num);
        } while (!gameCodes.add(code));
        return code;
    }

    public void start(Game game) {
        game.setStatus(GameStatus.STARTED);
        game.setStartTime(OffsetDateTime.now());
        game = initGameTasks(game);
        gameRepository.save(game);
        log.info("{}| Start game {}", game.getChatId(), game.getName());
        processGameTask(game, null);
    }

    public void sendStartMessage(Game game) {
        var helloMessageResponsePayloads = game.getQuest().getStartPayloads();
        if (helloMessageResponsePayloads == null) {
            log.warn("Unable to send hello message (null). Game: {}, Quest: {}", game.getName(), game.getQuest().getName());
            return;
        }
        for (var payload : game.getQuest().getStartPayloads()) {
            BotEvent botEvent = BotEvent.builder()
                    .chatId(game.getChatId())
                    .message(payload.getText())
                    .payloadType(payload.getType())
                    .resourceName(payload.getResourceName())
                    .pin(false)
                    .build();
            eventPublisher.publishEvent(botEvent);
        }
    }

    public void stop(Game game) {
        GameTask gameTask = game.getCurrentTask();
        if (gameTask != null) {
            gameTask.setStatus(GameTaskStatus.STOPPED);
            gameTask.setEndTime(OffsetDateTime.now());
            if (gameTask.getStartTime() == null) {
                game.setStartTime(OffsetDateTime.now());
            }
//            GameTask newTask = createGameTask(game, gameTask.getQuestTask(), gameTask.getQuest(), gameTask.getIndex());
//            newTask.setId(gameTask.getId());
            gameTaskRepository.save(gameTask);
        }
        game.setStatus(GameStatus.STOPPED);
        gameRepository.save(game);
    }

    public void updateGameChatId(Long oldChatId, Long newChatId) {
        for (var game : gameRepository.findByChatId(oldChatId)) {
            game.setChatId(newChatId);
            if (!CollectionUtils.isEmpty(game.getTasks())) {
                for (var gameTask : game.getTasks()) {
                    gameTask.setChatId(newChatId);
                    gameTaskRepository.save(gameTask);
                }
            }
            gameRepository.save(game);
            log.info("Chat migration from {} to {} has been completed for {}", oldChatId, newChatId, game.getName());
        }
    }

    private void processGameTask(Game game, GameMessage gameMessage) {
        if (game.isTooManyPlayers()) {
            eventPublisher.publishEvent(BotEvent.text(game.getChatId(), "Слишком много игроков. Игра приостановлена."));
            return;
        }
        GameTask currentTask = game.getCurrentTask();
        if (currentTask == null) {
            finishGame(game);
            sendFinishGameNotifications(game);
            return;
        }
        if (gameMessage != null && systemMinPlayersCount > 0 && game.getPlayers().size() < systemMinPlayersCount) {
            log.info("Too few players ({}). Game: {}", game.getPlayers().size(), game.getName());
            if (!game.isMinPlayersNotificationSent()) {
                eventPublisher.publishEvent(BotEvent.text(game.getChatId(), "Вас слишком мало ☹️\nМинимальное количество игроков в команде - *" + systemMinPlayersCount + "*.\nДобавьте в чат еще игроков для продолжения игры."));
                game.setMinPlayersNotificationSent(true);
                gameRepository.save(game);
            }
            return;
        }
        gameTaskProcessor.processTask(currentTask, gameMessage);
    }

    private void finishGame(Game game) {
        for (GameTask gameTask : game.getTasks()) {
            gameTask.setStatus(GameTaskStatus.FINISHED);
            gameTaskRepository.save(gameTask);
        }
        game.setEndTime(OffsetDateTime.now());
        game.setStatus(GameStatus.FINISHED);
        int points = game.getTasks().stream().mapToInt(GameTask::getPoints).sum();
        game.setPoints(points);
        gameRepository.save(game);
        log.info("{}| Finish game {}", game.getChatId(), game.getName());
    }

    private void sendFinishGameNotifications(Game game) {
        if (game.getQuest().isShowResult()) {
            eventPublisher.publishEvent(BotEvent.text(game.getChatId(), String.format("Количество ваших победных очков: %s", game.getPoints())));
        }
        if (game.getQuest().getEndMessage() != null) {
            BotEvent botEvent = BotEvent.text(game.getChatId(), game.getQuest().getEndMessage());
            botEvent.setUnPin(true);
            eventPublisher.publishEvent(botEvent);
        }
    }

    private void setNextTask(Game game) {
        List<GameTask> gameTasks = game.getTasks();
        GameTask currentTask = game.getCurrentTask();
        if (currentTask.getIndex() >= gameTasks.size() - 1) {
            game.setCurrentTask(null);
        } else {
            game.setCurrentTask(gameTasks.get(currentTask.getIndex() + 1));
        }
        gameRepository.save(game);
    }

    private Game initGameFromQuest(Quest quest, GameMessage gameMessage, String competitionName) {
        Game game = new Game();
        game.setChatId(gameMessage.getChatId());
        game.setName(gameMessage.getChatName());
        game.setQuestName(quest.getName());
        game.setStatus(GameStatus.CREATED);
        game.setCreated(OffsetDateTime.now());
        game.setQuest(quest);
        game.setCompetition(competitionName);
        return gameRepository.save(game);
    }

    private Game initGameTasks(Game game) {
        Quest quest = questService.findByName(game.getQuestName());
        List<GameTask> gameTasks = new ArrayList<>();
        for (int i = 0; i < quest.getTasks().size(); i++) {
            gameTasks.add(createGameTask(game, quest.getTasks().get(i), quest, i));
        }
        gameTasks = gameTaskRepository.saveAll(gameTasks);
        game.setTasks(gameTasks);
        if (!gameTasks.isEmpty()) {
            game.setCurrentTask(gameTasks.get(0));
        }
        return gameRepository.save(game);
    }

    private GameTask createGameTask(Game game, QuestTask questTask, Quest quest, int index) {
        return GameTask.builder()
                .gameId(game.getId())
                .status(GameTaskStatus.CREATED)
                .chatId(game.getChatId())
                .questTask(questTask)
                .type(questTask.getType())
                .quest(quest)
                .wait(questTask.isStartByCommand())
                .competition(game.getCompetition())
                .index(index).build();
    }

    public void addGameMembers(GameMembersChangedMessage gameMembersChangedMessage) {
        var game = gameRepository.findByChatId(gameMembersChangedMessage.getChatId())
                .stream().filter(g -> g.getStatus() != GameStatus.FINISHED).findFirst().orElse(null);
        if (game == null) {
            log.warn("Unable to find game to add members. Chat Id: {}", gameMembersChangedMessage.getChatId());
            return;
        }
//        if (game.getMaxPlayers() == null) {
//            log.info("Max players is not set for game {}", game.getName());
//            return;
//        }
        Set<Player> players = new LinkedHashSet<>(game.getPlayers());
        players.addAll(gameMembersChangedMessage.getChangedPlayers().stream().filter(p -> !admins.contains(p.getUsername())).collect(Collectors.toList()));
        game.setPlayers(new ArrayList<>(players));
        if (game.getMaxPlayers() != null && game.getPlayers().size() > game.getMaxPlayers()) {
            eventPublisher.publishEvent(BotEvent.text(game.getChatId(), "Стало тесновато :)\nМаксимальное кол-во участников: *" + game.getMaxPlayers() + "*.\n\nПожалуйста, удалите лишних игроков из чата, чтобы начать/продолжить игру."));
            game.setTooManyPlayers(true);
            log.info("Game '{}' has been blocked. Current player count: {}, max count: {}", game.getName(), game.getPlayers().size(), game.getMaxPlayers());
        }
        if (systemMinPlayersCount > 0 && game.isMinPlayersNotificationSent() && game.getPlayers().size() >= systemMinPlayersCount) {
            eventPublisher.publishEvent(BotEvent.text(game.getChatId(), "Супер! Можем продолжать!"));
            game.setMinPlayersNotificationSent(false);
        }
//        if (game.getMaxPlayers() != null && players.size() > game.getMaxPlayers()) {
//            eventPublisher.publishEvent(BotEvent.text(game.getChatId(), "Стало тесновато :)\nКоличество участников в игре не должно быть больше " + game.getMaxPlayers()));
//            List<Integer> userIds = players.stream().map(Player::getId).collect(Collectors.toList());
//            Collections.reverse(userIds);
//            for (int i = 0; i < players.size() - game.getMaxPlayers(); i++) {
//                if (i >= userIds.size()) {
//                    return;
//                }
//                log.info("Kicking player {} from chat {} ({})", userIds.get(i), game.getChatId(), game.getName());
//                final Integer userId = userIds.get(i);
//                eventPublisher.publishEvent(new KickChatMemberEvent(gameMembersChangedMessage.getChatId(), userIds.get(i)));
//                eventPublisher.publishEvent(BotEvent.text(game.getChatId(), "Удален игрок " + players.stream().filter(p -> p.getId().equals(userId)).findFirst().get().getName())));
//                players = players.stream().filter(p -> !p.getId().equals(userId)).collect(Collectors.toSet());
//
//            }
//        }

        gameRepository.save(game);
    }

    public void deleteGameMembers(GameMembersChangedMessage gameMembersChangedMessage) {
        var game = gameRepository.findByChatId(gameMembersChangedMessage.getChatId())
                .stream().filter(g -> g.getStatus() != GameStatus.FINISHED).findFirst().orElse(null);
        if (game == null) {
            log.warn("Unable to find game to add members. Chat Id: {}", gameMembersChangedMessage.getChatId());
            return;
        }
//        if (game.getMaxPlayers() == null) {
//            log.info("Max players is not set for game {}", game.getName());
//            return;
//        }
        game.setPlayers(game.getPlayers().stream().filter(p -> !p.getId().equals(gameMembersChangedMessage.getChangedPlayers().get(0).getId())).collect(Collectors.toList()));
        gameRepository.save(game);
        if (game.getMaxPlayers() != null && game.isTooManyPlayers() && game.getPlayers().size() <= game.getMaxPlayers()) {
            eventPublisher.publishEvent(BotEvent.text(game.getChatId(), "Теперь все ОК. Спасибо! :)"));
            game.setTooManyPlayers(false);
            log.info("Game '{}' has been blocked. Current player count: {}, max count: {}", game.getName(), game.getPlayers(), game.getMaxPlayers());
            gameRepository.save(game);
        }
    }

    public void addPoints(Game game, Integer taskNumber, Integer points) {
        GameTask task = game.getTasks().stream().filter(g -> taskNumber.equals(g.getIndex())).findFirst().orElse(null);
        if (task == null) {
            log.info("Task with index {} not found in game {}", taskNumber, game.getName());
            return;
        }
        task.setPoints(task.getPoints() + points);
        gameTaskRepository.save(task);
        Game newGame = gameRepository.findById(game.getId()).get();
        int newPoints = newGame.getTasks().stream().mapToInt(GameTask::getPoints).sum();
        newGame.setPoints(newPoints);
        gameRepository.save(newGame);

    }

    public void nextTask(Game game) {
        GameTask gameTask = game.getCurrentTask();
        if (gameTask == null) {
            log.warn("Unable to start next task in game {}. No current task", game.getName());
            return;
        }
        if (gameTask.getStatus() == GameTaskStatus.STARTED) {
            log.info("Finishing task {} in game {} due to requested next task", gameTask.getQuestTask().getName(), game.getName());
            gameTask.setStatus(GameTaskStatus.FINISHED);
            gameTask.setEndTime(OffsetDateTime.now());
            gameTaskRepository.save(gameTask);
            int points = game.getTasks().stream().mapToInt(GameTask::getPoints).sum();
            game.setPoints(points);
            setNextTask(game);
            gameTask = game.getCurrentTask();
        }
        if (gameTask != null) {
            gameTask.setWait(false);
            gameTaskRepository.save(gameTask);
        }
        processGameTask(gameRepository.findById(game.getId()).get(), null);
    }

    public Game getGameToInviteByGameCode(String registrationCode) throws GameNotFoundException, GamePlayersLimitException {
        Game game = gameRepository.findFirstByRegistrationCode(registrationCode);
        if (game == null) {
            throw new GameNotFoundException("Game not found by code phrase " + registrationCode);
        }
        if (game.getMaxPlayers() != null && game.getInvites() >= game.getMaxPlayers()) {
            throw new GamePlayersLimitException("Game is full. Max players: " + game.getMaxPlayers());
        }
        game.setInvites(game.getInvites() + 1);
        return gameRepository.save(game);
    }


    public Game getGameToInviteByCompetition(String competition, Integer maxPlayers, Integer batchSize) throws GameNotFoundException, CompetitionPlayersLimitException {
        List<Game> games = gameRepository.findByCompetitionAndDisabledIsFalse(competition);
        if (games == null || games.isEmpty()) {
            throw new GameNotFoundException("No games found by competition " + competition);
        }

        games.sort(Comparator.comparing(Game::getInvites, Comparator.reverseOrder()));
        List<List<Game>> batches = ListUtils.partition(games, batchSize);
        for (List<Game> batch : batches) {
            batch.sort(Comparator.comparing(Game::getInvites));
            for (Game game : batch) {
                if (maxPlayers != null) {
                    if (game.getInvites() < maxPlayers) {
                        game.setInvites(game.getInvites() + 1);
                        return gameRepository.save(game);
                    }
                } else {
                    game.setInvites(game.getInvites() + 1);
                    return gameRepository.save(game);
                }
            }
        }
        throw new CompetitionPlayersLimitException("All games are full in competition " + competition);
    }

    public void cancelInvite(Long chatId) {
        List<Game> games = gameRepository.findByChatId(chatId);
        for (Game game : games) {
            log.info("Cancel invite for game with chat id {}", chatId);
            game.setInvites(game.getInvites() - 1);
            gameRepository.save(game);
        }
    }
}
