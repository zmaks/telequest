package com.mzheltoukhov.questpistols.model.task;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaFormat;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaTitle;
import com.mzheltoukhov.questpistols.model.PayloadType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(value = {"id"})
@JsonSchemaFormat("tabs-top")
public class QuestTaskAnswer {

    private Integer id;

    /**
     * true - this answer isn't taken into account during checking the task.
     * it's just a simple action
     */
    @JsonSchemaTitle("Не добавлять очки")
    private boolean ignoreInCounting;

    /**
     * Variations of users' answer. i.e. ['Alex Pushkin', 'Pushkin Alex', 'A. Pushkin']
     */
    @JsonSchemaTitle("Список вариантов")
    private List<String> text;

    /**
     * For ForwardingTask - reaction on file
     */
    @JsonSchemaTitle("Тип ответа")
    private PayloadType answerPayloadType;

    /**
     * true - the text is case sensitive, spaces are considered.
     */
    @JsonSchemaTitle("Строгая проверка совпадения")
    private boolean strict;

    /**
     * Similar as 'positiveReactions'. i.e. bot's reactions to the correct answer
     */
    @JsonSchemaTitle("Реакция на правильный ответ")
    private List<ResponsePayload> reactions;

    /**
     * points amount for the correct answer. may be positive/negative
     */
    @JsonSchemaTitle("Очки")
    private Integer points;

    //totalPoints = points - spentTime/pointsTime  *  finePoints
    @JsonSchemaTitle("Период уменьшения очков")
    private Integer pointsTime;

    @JsonSchemaTitle("Кол-во уменьшения")
    private Integer finePoints;

    @JsonSchemaTitle("Заканчивать по этому ответу в любом случае")
    private boolean finalAnswer = false;

    @JsonSchemaTitle("Учитывать очки, но не учитывать в количестве отвеченных")
    private boolean bonus = false;
}
