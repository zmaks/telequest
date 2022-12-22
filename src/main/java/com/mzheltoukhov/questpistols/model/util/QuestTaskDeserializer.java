package com.mzheltoukhov.questpistols.model.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.mzheltoukhov.questpistols.model.task.QuestTask;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.core.ParameterizedTypeReference;

import java.io.IOException;
import java.util.List;

public class QuestTaskDeserializer extends JsonDeserializer<List<QuestTask>> {
    private TypeReference<List<QuestTask>> typeListOfQuestTasks = new TypeReference<List<QuestTask>>() {};

    @Override
    public List<QuestTask> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        ObjectCodec objectCodec = jsonParser.getCodec();
        List<QuestTask> questTasks = objectCodec.readValue(jsonParser, typeListOfQuestTasks);
        return questTasks;
    }
}
