package com.mzheltoukhov.questpistols.model.event.bot;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.ByteArrayOutputStream;

@Data
@AllArgsConstructor
public class SendFileEvent {
    private Long chatId;
    private Integer replyTo;
    private String fileName;
    private ByteArrayOutputStream stream;
}
