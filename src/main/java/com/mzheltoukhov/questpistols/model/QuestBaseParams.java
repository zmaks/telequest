package com.mzheltoukhov.questpistols.model;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaFormat;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaTitle;
import com.mzheltoukhov.questpistols.model.task.ResponsePayload;
import lombok.*;

import java.util.Arrays;
import java.util.List;

@Builder
@Data
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class QuestBaseParams {

    @JsonSchemaTitle("Очки")
    @JsonPropertyDescription("Кол-во очков за задание. Может быть отрицательным")
    private Integer points;

    @JsonSchemaTitle("Штрафные очки")
    @JsonPropertyDescription("Максимальное значение начисляемых штрафных очков. " +
            "Начисленые штрафные очки = (потраченное на задании время) * (штрафные очки) / (период начисления штрафных очков)")
    private Integer finePoints;

    @JsonSchemaTitle("Период начисления штрафных очков (сек)")
    @JsonPropertyDescription("Время, в течение которого будут насчитываться штрафные очки начиная со старта задания")
    private Integer pointsTimeSeconds;

    @JsonSchemaTitle("Продолжительность (сек)")
    @JsonPropertyDescription("Продолжительность задания в секундах")
    private Integer timeoutSeconds;

    @JsonSchemaTitle("Положительные реакции")
    @JsonPropertyDescription("Список сообщений, которые бот может прислать в ответ на правильный ответ")
    private List<String> positiveReactions;

    @JsonSchemaTitle("Отрецательные реакции")
    @JsonPropertyDescription("Список сообщений, которые бот может прислать в ответ на НЕправильный ответ")
    private List<String> negativeReactions;

    @JsonSchemaTitle("Таймаут реакции")
    @JsonPropertyDescription("Список сообщений, которые бот может прислать при истечении времени на задание (Продолжительность)")
    private List<String> timeoutReactions;

    @JsonSchemaTitle("Реакции повторных ответов")
    @JsonPropertyDescription("Список сообщений, которые бот может прислать в ответ на повторный ответ")
    private List<String> alreadyAnsweredReactions;

    @JsonSchemaTitle("Реакция исчерпания попыток")
    @JsonPropertyDescription("Сообщение, которое отправит бот, если попытки исчерпаны (в дополнение к отрицательной реакции)")
    private String attemptsExhaustedReaction;

    @JsonSchemaTitle("Финальное сообщение")
    @JsonPropertyDescription("Сообщение, которое отправит бот после окончания задания в дополнение к любой реакции")
    private String finalTaskReaction;

    private List<ResponsePayload> finalTaskReactionPayload;

    private Integer hintTimeInSeconds;
    private Integer hintFinePoints;
    private String hintAnnounceText;
    private String hintKeyWord;

    private static QuestBaseParams defaults = QuestBaseParams.builder()
            .positiveReactions(Arrays.asList("Правильно! \uD83D\uDCAA", "Верно! \uD83D\uDC4F", "Супер! \uD83D\uDC4D", "Да! \uD83D\uDC4F"))
            .negativeReactions(Arrays.asList("Неправильно \uD83D\uDE15", "Ответ неверный ☹️", "Нет \uD83D\uDE3F"))
            .timeoutReactions(Arrays.asList("Время вышло!⏰"))
            .alreadyAnsweredReactions(Arrays.asList("Такой ответ уже был."))
            //.attemptsExhaustedReaction("Увы. Попытки исчерпаны ☹️")
            .hintKeyWord("подсказка")
            .timeoutSeconds(-1)
            .points(1)
            .build();

    public static QuestBaseParams defaults() {
        return defaults;
    }
}
