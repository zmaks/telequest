package com.mzheltoukhov.questpistols.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PlayerStats {
    private String name;
    private String game;
    private int totalPoints;
    private int answerCount;


    public void addPoints(int points) {
        this.totalPoints += points;
    }

    public void addAnswer() {
        this.answerCount = this.answerCount + 1;
    }
}
