package com.mzheltoukhov.questpistols.configuration.proxy;

import com.mzheltoukhov.questpistols.service.processor.GameTaskProcessor;
import com.mzheltoukhov.questpistols.service.processor.TaskProcessor;
import com.mzheltoukhov.questpistols.service.processor.proxy.GameTaskProcessorProxy;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.lang.reflect.Proxy;
import java.util.List;

@Configuration
public class ProxyConfiguration {

    @Bean
    @Primary
    public TaskProcessor gameTaskProcessor(List<TaskProcessor> gameTaskProcessorList) {
        return (TaskProcessor) Proxy.newProxyInstance(
                TaskProcessor.class.getClassLoader(),
                new Class<?>[]{TaskProcessor.class},
                new GameTaskProcessorProxy(gameTaskProcessorList)
        );
    }
}
