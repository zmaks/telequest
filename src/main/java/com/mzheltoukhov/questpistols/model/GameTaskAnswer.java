package com.mzheltoukhov.questpistols.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GameTaskAnswer {
    private Integer questAnswerId;
    private String answer;
    private String player;
    private OffsetDateTime createdTime;
    private int points;
    private boolean bonus;
}
