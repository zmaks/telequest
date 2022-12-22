package com.mzheltoukhov.questpistols.model;

import com.mzheltoukhov.questpistols.service.processor.ForwardingTaskProcessor;
import com.mzheltoukhov.questpistols.service.processor.GameTaskProcessor;
import com.mzheltoukhov.questpistols.service.processor.SharedDataTaskProcessor;
import com.mzheltoukhov.questpistols.service.processor.SimpleQuestionTaskProcessor;

public enum TaskType {
    SIMPLE_QUESTION(SimpleQuestionTaskProcessor.class),
    FORWARDING(ForwardingTaskProcessor.class),
    SHARED_DATA(SharedDataTaskProcessor.class);

    TaskType(Class<? extends GameTaskProcessor> processorClass) {
        this.processorClass = processorClass;
    }

    private final Class<? extends GameTaskProcessor> processorClass;

    public Class<? extends GameTaskProcessor> getProcessorClass() {
        return processorClass;
    }
}
