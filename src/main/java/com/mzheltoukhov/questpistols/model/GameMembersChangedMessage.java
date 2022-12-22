package com.mzheltoukhov.questpistols.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class GameMembersChangedMessage {
    private Long chatId;
    private Integer currentCount;
    private List<Player> changedPlayers;
}
