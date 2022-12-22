package com.mzheltoukhov.questpistols.configuration.mongo;

import org.springframework.core.convert.converter.Converter;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

public class DateToOffsetDateTimeConverter implements Converter<Date, OffsetDateTime> {
    @Override
    public OffsetDateTime convert(Date source) {
        return source == null ? null : ZonedDateTime.ofInstant(source.toInstant(), ZoneOffset.systemDefault()).toOffsetDateTime();
    }
}
