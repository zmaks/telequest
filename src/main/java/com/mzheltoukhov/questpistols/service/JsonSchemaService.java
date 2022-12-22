package com.mzheltoukhov.questpistols.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kjetland.jackson.jsonSchema.JsonSchemaConfig;
import com.kjetland.jackson.jsonSchema.JsonSchemaGenerator;
import com.mzheltoukhov.questpistols.model.Quest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class JsonSchemaService {

    @Autowired
    private ObjectMapper mapper;

    @PostConstruct
    public void createSchema() throws IOException {
        JsonSchemaGenerator jsonSchemaGenerator = new JsonSchemaGenerator(mapper, JsonSchemaConfig.html5EnabledSchema());
        JsonNode jsonSchema = jsonSchemaGenerator.generateJsonSchema(Quest.class);
        String jsonSchemaAsString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonSchema);
        FileUtils.write(new File("quest-json-schema.json"), jsonSchemaAsString, StandardCharsets.UTF_8);
        log.info("JSON-schema has been created");
    }
}
