package com.mzheltoukhov.questpistols.configuration.mongo;

import org.springframework.core.convert.converter.Converter;

import java.time.OffsetDateTime;
import java.util.Date;

public class OffsetDateTimeToDateConverter  implements Converter<OffsetDateTime, Date> {
    @Override
    public Date convert(OffsetDateTime source) {
        return source == null ? null : Date.from(source.toInstant());
    }
}
