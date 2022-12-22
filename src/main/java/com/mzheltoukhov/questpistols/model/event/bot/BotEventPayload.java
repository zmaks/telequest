package com.mzheltoukhov.questpistols.model.event.bot;

import com.mzheltoukhov.questpistols.model.PayloadType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
public class BotEventPayload {

    private PayloadType payloadType;
    private String fileId;
    private String resourceName;
}
