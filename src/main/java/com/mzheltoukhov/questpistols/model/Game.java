package com.mzheltoukhov.questpistols.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Document("games")
@Data
public class Game {
    @Id
    private String id;

    private String name;

    private String questName;

    @Indexed
    private Long chatId;

    @Indexed
    private GameStatus status = GameStatus.CREATED;

    private OffsetDateTime startTime;

    private OffsetDateTime endTime;

    private OffsetDateTime created;

    @DBRef
    private List<GameTask> tasks;

    private Integer maxPlayers;

    private boolean minPlayersNotificationSent;

    private String competition;

    private int points;

    @DBRef
    private GameTask currentTask;

    @DBRef(lazy = true)
    private Quest quest;

    private List<Player> players = new ArrayList<>();

    boolean tooManyPlayers;

    boolean disabled;

    String registrationCode;

    private OffsetDateTime lastInvitedAt;

    private int invites;

}
