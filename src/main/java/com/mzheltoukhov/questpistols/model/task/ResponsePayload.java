package com.mzheltoukhov.questpistols.model.task;

import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaFormat;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaTitle;
import com.mzheltoukhov.questpistols.model.PayloadType;
import lombok.Data;

import java.util.List;

@Data
public class ResponsePayload {
    @JsonSchemaTitle("Тип содержимого")
    private PayloadType type;
    @JsonSchemaTitle("Текст содержимого")
    private String text;
    @JsonSchemaTitle("Имя ресура")
    private String resourceName;
    private List<QuestTaskAnswerButton> buttons;
}
