package com.mzheltoukhov.questpistols.service;

import com.mzheltoukhov.questpistols.exception.*;
import com.mzheltoukhov.questpistols.model.*;
import com.mzheltoukhov.questpistols.repository.CompetitionRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CompetitionService {

    private static final int WIDTH_FACTOR = 210;

    private final GameService gameService;
    private final QuestService questService;
    private final CompetitionRepository competitionRepository;

    @Autowired
    public CompetitionService(GameService gameService, QuestService questService, CompetitionRepository competitionRepository) {
        this.gameService = gameService;
        this.questService = questService;
        this.competitionRepository = competitionRepository;
    }

    public Competition create(String name, Integer maxPlayers, Long creatorId, String codePhrase, Integer batchSize) throws CompetitionExistsException, DefaultQuestNotFoundException {
        if (competitionRepository.findByName(name) != null) {
            throw new CompetitionExistsException("Duplicate name " + name);
        }
        Quest quest = questService.getDefaultQuest();
        var competition = new Competition();
        competition.setName(name);
        competition.setQuestName(quest.getName());
        competition.setStatus(Competition.Status.CREATED);
        competition.setCreatedTime(OffsetDateTime.now());
        competition.setMaxPlayers(maxPlayers);
        competition.setCreatedById(creatorId);
        if (codePhrase != null) {
            competition.setCodePhrase(codePhrase.toLowerCase());
        }
        competition.setInviteBatchSize(batchSize);
        log.info("Created competition: {}", competition);
        return competitionRepository.save(competition);
    }

    public Competition findLastCompetition() {
        return competitionRepository.findTopByEnabledIsTrueOrderByCreatedTimeDesc();
    }

    public Game addToCompetition(Competition competition, GameMessage gameMessage) throws GameAlreadyExistsException {
        var game = gameService.create(gameMessage, competition.getMaxPlayers(), competition.getName(), competition.getQuestName());
        competition.addGame(game);
        competitionRepository.save(competition);
        log.info("{} - game has been added to competition - {}", game.getName(), competition.getName());
        if (game.getQuest().isSendStartPayloadsAutomatically()) {
            log.info("Sending start message {} , isSendStartPayloadsAutomatically==true", game.getName());
            gameService.sendStartMessage(game);
        }
        if (competition.getStatus().equals(Competition.Status.STARTED)) {
            log.info("Starting {} in competition - {}", game.getName(), competition.getName());
            gameService.start(game);
        }
        return game;
    }

    public Competition start(String competitionName) throws CompetitionNotFoundException, CompetitionAlreadyStartedException {
        var competition = findByName(competitionName);
        if (competition.getStatus().equals(Competition.Status.STARTED)) {
            throw new CompetitionAlreadyStartedException("Status == STARTED");
        }
        competition.setStatus(Competition.Status.STARTED);
        competition.setStartTime(OffsetDateTime.now());
        competition = competitionRepository.save(competition);
        for (var game : competition.getGames()) {
            log.info("Starting {} in competition - {}", game.getName(), competition.getName());
            gameService.start(game);
            sleep(200);
        }
        return competition;
    }

    public void sendStartMessage(String competitionName) throws CompetitionNotFoundException, CompetitionAlreadyStartedException {
        var competition = findByName(competitionName);
        if (competition.getStatus().equals(Competition.Status.STARTED)) {
            throw new CompetitionAlreadyStartedException("Status == STARTED");
        }
        for (var game : competition.getGames()) {
            gameService.sendStartMessage(game);
            sleep(100);
        }
    }

    public List<String> status(String competitionName) throws CompetitionNotFoundException {
        var competition = findByName(competitionName);
        return competition.getGames().stream().sorted(Comparator.comparing(Game::getPoints).reversed())
                .map(g -> {
                    String text = "--";
                    if (g.getStatus().equals(GameStatus.CREATED)) {
                        text = "Не начали";
                    }
                    if (g.getStatus().equals(GameStatus.STARTED)) {
                        text = "На задании '" + g.getCurrentTask().getQuestTask().getName() + "' - " + g.getPoints() + " очков";
                    }
                    if (g.getStatus().equals(GameStatus.FINISHED)) {
                        text = "Закончили - " + g.getPoints() + " очков";
                    }
                    return String.format("*%s* - %s", g.getName(), text);
                }).collect(Collectors.toList());
    }

    public void addPoints(String competitionName, String gameName, Integer taskNumber, Integer points) throws CompetitionNotFoundException {
        var competition = findByName(competitionName);
        Game game = competition.getGames().stream().filter(g -> StringUtils.deleteWhitespace(g.getName()).equalsIgnoreCase(gameName)).findFirst().orElseThrow(() -> new IllegalArgumentException("В соревновании нет такой игры " + gameName));
        gameService.addPoints(game, taskNumber, points);
    }

    public ByteArrayOutputStream generateExcelReport(String competitionName) throws CompetitionNotFoundException {
        var competition = findByName(competitionName);
        var wb = new HSSFWorkbook();
        var sheet = wb.createSheet(competitionName);
        sheet.setDefaultRowHeightInPoints(18);
        var quest = questService.findByName(competition.getQuestName());
        var rowNumber = 0;
        var headerRow = sheet.createRow(rowNumber);
        int headerCellNumber = 0;

        HSSFCellStyle cellStyle = wb.createCellStyle();
        cellStyle.setBorderBottom(BorderStyle.MEDIUM);
        cellStyle.setBorderLeft(BorderStyle.MEDIUM);
        cellStyle.setBorderRight(BorderStyle.MEDIUM);
        cellStyle.setBorderTop(BorderStyle.MEDIUM);
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        HSSFFont font = wb.createFont();
        font.setBold(true);
        cellStyle.setFont(font);

        var posCell = headerRow.createCell(headerCellNumber);
        posCell.setCellValue("#");
        posCell.setCellStyle(cellStyle);

        var nameCell = headerRow.createCell(++headerCellNumber);
        nameCell.setCellValue("Название команды");
        nameCell.setCellStyle(cellStyle);

        var timeColumnIndexes = new ArrayList<Integer>();
        var pointsColumnIndexes = new ArrayList<Integer>();
        for (var questTask : quest.getTasks()) {
            var cell1 = headerRow.createCell(++headerCellNumber);
            timeColumnIndexes.add(headerCellNumber);
            cell1.setCellStyle(cellStyle);
            var cell2 = headerRow.createCell(++headerCellNumber);
            pointsColumnIndexes.add(headerCellNumber);
            cell2.setCellStyle(cellStyle);
            cell1.setCellValue(questTask.getName());
        }
        var resultCell = headerRow.createCell(++headerCellNumber);
        resultCell.setCellValue("Итог");
        resultCell.setCellStyle(cellStyle);

        var games = competition.getGames().stream().sorted(Comparator.comparing(Game::getPoints).reversed()).collect(Collectors.toList());

        int gamePosition = 1;
        for (var game : games) {
            var gameRow = sheet.createRow(++rowNumber);
            var cellNumber = 0;

            var gamePosCell = gameRow.createCell(cellNumber);
            gamePosCell.setCellValue(gamePosition++);
            gamePosCell.setCellStyle(cellStyle);

            var gameNameCell = gameRow.createCell(++cellNumber);
            gameNameCell.setCellValue(game.getName());
            gameNameCell.setCellStyle(cellStyle);
            for (var gameTask : game.getTasks()) {
                var timeCell = gameRow.createCell(++cellNumber);
                var pointsCell = gameRow.createCell(++cellNumber);
                timeCell.setCellValue(getSpentTimeString(gameTask));
                pointsCell.setCellValue(gameTask.getPoints());
            }
            var gameResultCell = gameRow.createCell(++cellNumber);
            gameResultCell.setCellValue(game.getPoints());
            gameNameCell.setCellStyle(cellStyle);
        }

        //style

        for (var timeColIndex : timeColumnIndexes) {
            sheet.setColumnWidth(timeColIndex, 12 * WIDTH_FACTOR);
        }
        for (var pointsColIndex : pointsColumnIndexes) {
            sheet.setColumnWidth(pointsColIndex, 12 * WIDTH_FACTOR);
        }
        sheet.setColumnWidth(headerCellNumber, 10 * WIDTH_FACTOR); //sum result column

        //merge time and point columns in the header
        for (var timeColIndex : timeColumnIndexes) {
            sheet.addMergedRegion(new CellRangeAddress(headerRow.getRowNum(), headerRow.getRowNum(), timeColIndex, timeColIndex + 1));
        }

        for (int i = 0; i < 2; i++) {
            sheet.autoSizeColumn(i);
        }


        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            wb.write(byteArrayOutputStream);
            wb.close();
            return byteArrayOutputStream;
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    private String getSpentTimeString(GameTask gameTask) {
        var startTime = gameTask.getStartTime();
        if (startTime == null) {
            return "-";
        }
        var endTime = gameTask.getEndTime() == null ? OffsetDateTime.now() : gameTask.getEndTime();
        long totalSeconds = endTime.toEpochSecond() - startTime.toEpochSecond();
        var hours = totalSeconds / 3600;
        var minutes = (totalSeconds % 3600) / 60;
        var seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public void stop(String competitionName) throws CompetitionNotFoundException {
        var competition = findByName(competitionName);
        for (var game : competition.getGames()) {
            gameService.stop(game);
        }
    }

    public Competition findByName(String name) throws CompetitionNotFoundException {
        var competition = competitionRepository.findByName(name);
        if (competition == null) {
            throw new CompetitionNotFoundException(name + " - не найдено");
        }
        return competition;
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }

    public void nextTask(String competitionName) throws CompetitionNotFoundException {
        Competition competition = null;
        if (competitionName != null) {
            competition = competitionRepository.findByName(competitionName);
        }
        if (competition == null) {
            competition = findLastCompetition();
        }
        if (competition == null) {
            throw new CompetitionNotFoundException("Игра не найдена");
        }
        for (Game game : competition.getGames()) {
            gameService.nextTask(game);
            sleep(10);
        }
    }

    public Invite getGameInvite(String codePhrase) throws RegistrationException, IncorrectCodePhraseException {
        codePhrase = codePhrase.toLowerCase().replaceAll("\\s+", "").replace("ё", "е").replace("!", "");
        Competition competition = competitionRepository.findFirstByCodePhrase(codePhrase);
        try {
            Game game = gameService.getGameToInviteByGameCode(codePhrase);
            competition = competitionRepository.findByName(game.getCompetition());
            return new Invite(game.getChatId(), game.getRegistrationCode(), game.getName(), competition);
        } catch (GameNotFoundException e) {
            if (competition == null) {
                throw new IncorrectCodePhraseException("Неправильный регистрационный код");
            } else {
                try {
                    Game game = gameService.getGameToInviteByCompetition(competition.getName(), competition.getMaxPlayers(), competition.getInviteBatchSize());
                    return new Invite(game.getChatId(), game.getRegistrationCode(), game.getName(), competition);
                } catch (CompetitionPlayersLimitException ex) {
                    throw new RegistrationException(RegistrationException.MessageType.COMPETITION_IS_FULL);
                } catch (GameNotFoundException ex) {
                    throw new RegistrationException(RegistrationException.MessageType.GAME_NOT_STARTED_IN_COMPETITION);
                }
            }

        } catch (GamePlayersLimitException e) {
            throw new RegistrationException(RegistrationException.MessageType.GAME_IS_FULL);
        }
    }

    public void cancelGameInvite(Invite invite) {
        gameService.cancelInvite(invite.getInvitedToChat());
    }

    public ByteArrayOutputStream topPlayers(String competitionName) throws CompetitionNotFoundException {
        var competition = findByName(competitionName);
        Map<String, PlayerStats> statsMap = new HashMap<>();
        for (Game game : competition.getGames()) {
            for (GameTask gameTask : game.getTasks()) {
                for (GameTaskAnswer taskAnswer : gameTask.getAnswers()) {
                    if (statsMap.containsKey(taskAnswer.getPlayer())) {
                        statsMap.get(taskAnswer.getPlayer()).addPoints(taskAnswer.getPoints());
                        statsMap.get(taskAnswer.getPlayer()).addAnswer();
                    } else {
                        statsMap.put(taskAnswer.getPlayer(), new PlayerStats(taskAnswer.getPlayer(), game.getName(), taskAnswer.getPoints(), 1));
                    }
                }
            }
        }

        List<PlayerStats> stats = new ArrayList<>(statsMap.values());
        stats.sort(Comparator.comparing(PlayerStats::getTotalPoints).reversed());

        var wb = new HSSFWorkbook();
        var sheet = wb.createSheet(competitionName);
        sheet.setDefaultRowHeightInPoints(18);
        var rowNumber = 0;
        var headerRow = sheet.createRow(rowNumber);
        int headerCellNumber = 0;

        HSSFCellStyle cellStyle = wb.createCellStyle();
        cellStyle.setBorderBottom(BorderStyle.MEDIUM);
        cellStyle.setBorderLeft(BorderStyle.MEDIUM);
        cellStyle.setBorderRight(BorderStyle.MEDIUM);
        cellStyle.setBorderTop(BorderStyle.MEDIUM);
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        HSSFFont font = wb.createFont();
        font.setBold(true);
        cellStyle.setFont(font);

        var posCell = headerRow.createCell(headerCellNumber);
        posCell.setCellValue("#");
        posCell.setCellStyle(cellStyle);

        var nameCell = headerRow.createCell(++headerCellNumber);
        nameCell.setCellValue("Игрок");
        nameCell.setCellStyle(cellStyle);

        var answersCell = headerRow.createCell(++headerCellNumber);
        answersCell.setCellValue("Кол-во ответов");
        answersCell.setCellStyle(cellStyle);

        var pointsCell = headerRow.createCell(++headerCellNumber);
        pointsCell.setCellValue("Набранные очки");
        pointsCell.setCellStyle(cellStyle);

        var teamCell = headerRow.createCell(++headerCellNumber);
        teamCell.setCellValue("Название команды");
        teamCell.setCellStyle(cellStyle);


        int position = 1;
        for (var stat : stats) {
            var gameRow = sheet.createRow(++rowNumber);
            var cellNumber = 0;

            var gamePosCell = gameRow.createCell(cellNumber);
            gamePosCell.setCellValue(position++);
            gamePosCell.setCellStyle(cellStyle);

            var playerNameCell = gameRow.createCell(++cellNumber);
            playerNameCell.setCellValue(stat.getName());

            var playerAnsCell = gameRow.createCell(++cellNumber);
            playerAnsCell.setCellValue(stat.getAnswerCount());

            var playerPointsCell = gameRow.createCell(++cellNumber);
            playerPointsCell.setCellValue(stat.getTotalPoints());

            var playerTeamCell = gameRow.createCell(++cellNumber);
            playerTeamCell.setCellValue(stat.getGame());
        }


        for (int i = 0; i < headerCellNumber + 1; i++) {
            sheet.autoSizeColumn(i);
        }


        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            wb.write(byteArrayOutputStream);
            wb.close();
            return byteArrayOutputStream;
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }
}
