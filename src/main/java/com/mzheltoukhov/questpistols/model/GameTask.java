package com.mzheltoukhov.questpistols.model;

import com.mzheltoukhov.questpistols.model.task.QuestTask;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Document("gameTasks")
@Data
@Builder
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class GameTask {

    @Id
    private String id;

    private int index;

    private Long chatId;

    private int points;

    private int attempts;

    private OffsetDateTime startTime;

    private OffsetDateTime endTime;

    private OffsetDateTime hintTime;

    private boolean hintAnnounceSent;

    private boolean hintSent;

    private GameTaskStatus status = GameTaskStatus.CREATED;

    private boolean plugSent;

    private boolean finishedByTimeout;

    private List<GameTaskAnswer> answers = new ArrayList<>();

    private String gameId;

    @DBRef(lazy = true)
    private Quest quest;

    @DBRef
    private QuestTask questTask;

    private TaskType type;

    private boolean lock;

    private String competition;

    private Player playerToAnswer;

    private boolean playerToAnswerRemindSent;

    private boolean wait;

}
