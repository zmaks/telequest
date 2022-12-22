package com.mzheltoukhov.questpistols.configuration.mongo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.util.Arrays;

@Configuration
public class MongoConfiguration {

    @Bean
    public MongoCustomConversions customConversions() {
        return new MongoCustomConversions(Arrays.asList(
                new OffsetDateTimeToDateConverter(),
                new DateToOffsetDateTimeConverter()));
    }

}
