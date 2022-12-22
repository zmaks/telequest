package com.mzheltoukhov.questpistols.service.processor.proxy;

import com.mzheltoukhov.questpistols.model.GameTask;
import com.mzheltoukhov.questpistols.service.processor.GameTaskProcessor;
import com.mzheltoukhov.questpistols.service.processor.TaskProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class GameTaskProcessorProxy implements InvocationHandler {

    private Map<Class<? extends TaskProcessor>, TaskProcessor> processorMap;

    public GameTaskProcessorProxy(List<TaskProcessor> gameTaskProcessorList) {
        processorMap = gameTaskProcessorList.stream()
                .collect(Collectors.toMap(TaskProcessor::getClass, Function.identity()));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (!(args[0] instanceof GameTask)) {
            throw new IllegalStateException("First arg should be GameTask to proxy GameTaskProcessor");
        }
        GameTask gameTask = (GameTask) args[0];
        TaskProcessor processor = processorMap.get(gameTask.getType().getProcessorClass());
        if (processor == null) {
            throw new IllegalStateException("No processor found");
        }
        return method.invoke(processor, args);
    }
}
