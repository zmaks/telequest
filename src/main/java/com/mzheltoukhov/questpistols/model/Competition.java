package com.mzheltoukhov.questpistols.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Document("competitions")
@Data
public class Competition {

    public enum Status {
        CREATED, STARTED, STOPPED
    }

    @Id
    private String id;

    @Indexed(unique = true)
    private String name;

    private Integer maxPlayers;

    private OffsetDateTime startTime;

    private OffsetDateTime createdTime;

    private Long createdById;

    private Status status;

    private String questName;

    private String codePhrase;

    private int inviteBatchSize;

    @DBRef
    private List<Game> games = new ArrayList<>();

    private Boolean enabled = true;

    public void addGame(Game game) {
        this.games.add(game);
    }
}
