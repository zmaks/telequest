package com.mzheltoukhov.questpistols.model.task;

import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaTitle;
import lombok.Data;

import java.util.List;

@Data
public class QuestTaskPlug {
    @JsonSchemaTitle("Текст")
    private String text;
    @JsonSchemaTitle("Текст кнопки")
    private String button;
    @JsonSchemaTitle("Доп контент")
    private ResponsePayload payload;
    private List<ResponsePayload> payloads;
    private String answer;
}
