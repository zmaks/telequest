package com.mzheltoukhov.questpistols.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaFormat;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaTitle;
import com.mzheltoukhov.questpistols.model.task.QuestTask;
import com.mzheltoukhov.questpistols.model.task.ResponsePayload;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Document("quests")
@Data
@JsonIgnoreProperties(value = {"id", "createdByChatId", "createdTime"})
@JsonPropertyOrder({"name", "keyWords", "endMessage", "baseParams", "showResult", "tasks"})
public class Quest {
    @Id
    private String id;

    private Long createdByChatId;

    private OffsetDateTime createdTime;

    @NotBlank
    @Size(min = 3, message = "Необходимо минимум 3 символа")
    @JsonSchemaTitle("Имя квеста")
    private String name;

    @JsonSchemaTitle("Ключевые слова")
    @JsonPropertyDescription("Слова по которым можно стартануть квест (передается в команде 'старт')")
    private List<String> keyWords;

    @JsonSchemaTitle("Вводное сообщение")
    @JsonPropertyDescription("Присылается ботом перед игрой")
    @Deprecated
    private String startMessage;

    @JsonSchemaTitle("Вводные сообщения")
    @JsonPropertyDescription("Присылается ботом перед игрой")
    private List<ResponsePayload> startPayloads;

    @JsonSchemaTitle("Отправить вводные сообщения")
    @JsonPropertyDescription("Отправить вводные сообщения автоматически при подключении к игре")
    private boolean sendStartPayloadsAutomatically;

    @JsonSchemaTitle("Эпилог квеста")
    @JsonPropertyDescription("Присылается ботом после прохождения всех заданий")
    private String endMessage;

    @JsonSchemaTitle("Параметры квеста")
    @JsonPropertyDescription("Набор второстепенных параметров для заданий (реакции, очки и т.д.). Распространяется на все задания. Может быть переопределено")
    private QuestBaseParams baseParams;

    @JsonSchemaTitle("Показывать набранные очки")
    private boolean showResult;

    @JsonSchemaTitle("Задания квеста")
    @JsonPropertyDescription("Задания, выдаваемые ботом. Порядок заданий сохраняется")
    @JsonSchemaFormat("tabs-top")
    @DBRef
    private List<QuestTask> tasks = new ArrayList<>();

    private Integer durationMinutes;

}
