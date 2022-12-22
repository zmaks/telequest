package com.mzheltoukhov.questpistols.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GameMessagePayload {
    private PayloadType type;
    private String fileId;
}
