package com.mzheltoukhov.questpistols.model.event.bot;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.InputStream;
import java.util.function.Consumer;

@Data
@AllArgsConstructor
public class DownloadFileEvent {
    private String fileId;
    private Consumer<InputStream> fileConsumer;

}
