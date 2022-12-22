package com.mzheltoukhov.questpistols.model.event.bot;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class BotEventButton {
    private String name;
    private String data;
}
