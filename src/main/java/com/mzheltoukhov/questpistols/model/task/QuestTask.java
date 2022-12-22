package com.mzheltoukhov.questpistols.model.task;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaFormat;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaTitle;
import com.mzheltoukhov.questpistols.model.PayloadType;
import com.mzheltoukhov.questpistols.model.QuestBaseParams;
import com.mzheltoukhov.questpistols.model.SharedDataTaskType;
import com.mzheltoukhov.questpistols.model.TaskType;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Document("tasks")
@Data
@JsonIgnoreProperties(value = {"id", "questName"})
public class QuestTask {
    @Id
    private String id;

    @NotBlank
    @JsonSchemaTitle("Имя задания")
    private String name;

    @NotBlank
    @JsonSchemaTitle("Ключевые слова")
    private TaskType type;

    @JsonSchemaTitle("Заглушка")
    @JsonPropertyDescription("Присылается в начале задания, чтобы дать игрокам время подготовиться. Время пока висит заглушка не учитывается.")
    private QuestTaskPlug plug;

    @JsonSchemaTitle("Описания")
    @JsonPropertyDescription("Набор описаний задания. Присылаются по очереди отдельными сообщениями")
    @JsonSchemaFormat("tabs-top")
    private List<ResponsePayload> descriptions;

    @JsonSchemaTitle("Закрепить первое сообщение описания заданий")
    private boolean pinFirst = true;

    @JsonSchemaTitle("Ответы")
    @JsonPropertyDescription("Список ответов на задание")
    @JsonSchemaFormat("tabs-top")
    private List<QuestTaskAnswer> answers;

    @JsonSchemaTitle("Попытки")
    @JsonPropertyDescription("Кол-во попыток")
    private Integer attempts;

    @JsonSchemaTitle("Штраф за попытку")
    private Integer attemptFine;

    @JsonSchemaTitle("Кол-во ответов")
    @JsonPropertyDescription("Необходиое кол-во правильных ответов")
    private Integer requiredCorrectAnswers = 1;

    @JsonSchemaTitle("Игнор уже отвеченных")
    private boolean ignoreAlreadyAnswered;

    @JsonSchemaTitle("Параметры задания")
    private QuestBaseParams baseParams;

    private boolean choosePlayerToAnswer;

    private TaskHint hint;

    private String questName;

    private boolean startByCommand;

    private boolean neutralReaction;

    private boolean allowSkippingBonus;

    //ForwardingTask

    @JsonSchemaTitle("Переадресация")
    private PayloadType requiredFileType;

    @JsonSchemaTitle("Переадресация кому")
    private List<Long> forwardingTargetUsers;

    //SharedDatTask

    private SharedDataTaskType sharedDataTaskType;
    @JsonSchemaTitle("На сколько уменьшаяются баллы в зависимости от рейтинга ответа")
    private Integer stepPoint = 1;
}
