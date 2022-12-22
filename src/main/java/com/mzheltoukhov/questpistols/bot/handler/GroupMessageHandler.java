package com.mzheltoukhov.questpistols.bot.handler;

import com.mzheltoukhov.questpistols.exception.CompetitionNotFoundException;
import com.mzheltoukhov.questpistols.exception.GameAlreadyExistsException;
import com.mzheltoukhov.questpistols.model.Competition;
import com.mzheltoukhov.questpistols.model.Game;
import com.mzheltoukhov.questpistols.model.GameMessage;
import com.mzheltoukhov.questpistols.model.GameMembersChangedMessage;
import com.mzheltoukhov.questpistols.model.event.bot.BotEvent;
import com.mzheltoukhov.questpistols.service.CompetitionService;
import com.mzheltoukhov.questpistols.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.List;

@Component
public class GroupMessageHandler {

    @Autowired
    private GameService gameService;
    @Autowired
    private CompetitionService competitionService;
    @Autowired
    private ApplicationEventPublisher eventPublisher;


    @Value("#{'${bot.admins:z_maks}'.split(',')}")
    private List<String> admins;

    public void handle(Message message) {
        if (message.getLeftChatMember() != null) {
            //TODO stop game if bot deleted
        }

        if (message.getFrom() != null && message.getFrom().getUserName() != null && admins.contains(message.getFrom().getUserName())) {
            if (message.getText() != null && message.getText().startsWith("/add")) {
                executeAddGameCommand(message);
                return;
            }
            if (message.getGroupchatCreated() != null && message.getGroupchatCreated()) {
                //TODO для new chat member
                executeAddGameCommand(message);
                return;
            }
        }

        gameService.handleGameMessage(GameMessage.fromTelegramMessage(message));
    }

    private void executeAddGameCommand(Message message) {

        try {
            Competition competition;
            if (message.getText() != null) {
                var parts = message.getText().split(" ");
                if (parts.length != 2) {
                    eventPublisher.publishEvent(BotEvent.text(message.getChatId(), "Ошибка"));
                    return;
                }
                String competitionName = parts[1];
                competition = competitionService.findByName(competitionName);

            } else {
                competition = competitionService.findLastCompetition();
            }
            Game game = competitionService.addToCompetition(competition, GameMessage.fromTelegramMessage(message));
            eventPublisher.publishEvent(BotEvent.text(message.getChatId(), "\uD83D\uDC4D"));
            eventPublisher.publishEvent(BotEvent.text((long) message.getFrom().getId(), String.format("Чат '*%s*' добавлен в *%s*", game.getName(), competition.getName())));
        } catch (CompetitionNotFoundException e) {
            eventPublisher.publishEvent(BotEvent.text(message.getChatId(), "Нет такой игры"));
        } catch (GameAlreadyExistsException e) {
            eventPublisher.publishEvent(BotEvent.text(message.getChatId(), "Ошибка. Игра уже создана"));
        }
    }

    public void handleNewMembers(GameMembersChangedMessage gameMembersChangedMessage) {
        gameService.addGameMembers(gameMembersChangedMessage);
    }

    public void handleLeftMembers(GameMembersChangedMessage gameMembersChangedMessage) {
        gameService.deleteGameMembers(gameMembersChangedMessage);
    }
}
