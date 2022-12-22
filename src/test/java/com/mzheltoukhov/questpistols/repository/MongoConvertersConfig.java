package com.mzheltoukhov.questpistols.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mzheltoukhov.questpistols.configuration.mongo.DateToOffsetDateTimeConverter;
import com.mzheltoukhov.questpistols.configuration.mongo.OffsetDateTimeToDateConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class MongoConvertersConfig {
    @Bean
    public MongoCustomConversions customConversions() {
        return new MongoCustomConversions(Arrays.asList(
                new OffsetDateTimeToDateConverter(),
                new DateToOffsetDateTimeConverter()));
    }
}
