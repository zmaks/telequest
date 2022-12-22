package com.mzheltoukhov.questpistols.bot.model;

import com.mzheltoukhov.questpistols.model.PayloadType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.OffsetDateTime;

@Data
@Builder
@Document("resources")
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class TelegramResource {
    @Id
    private String id;
    @Indexed(unique = true)
    private String name;
    private String fileId;
    private TelegramResourceType type;
    private Long createdByChatId;
    private OffsetDateTime createdTime;
}
