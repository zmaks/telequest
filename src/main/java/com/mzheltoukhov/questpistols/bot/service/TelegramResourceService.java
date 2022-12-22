package com.mzheltoukhov.questpistols.bot.service;

import com.mzheltoukhov.questpistols.bot.model.TelegramResource;
import com.mzheltoukhov.questpistols.bot.model.TelegramResourceType;
import com.mzheltoukhov.questpistols.bot.repository.TelegramResourceRepository;
import com.mzheltoukhov.questpistols.model.event.bot.BotEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;

@Slf4j
@Service
public class TelegramResourceService {
    public static final String ADD_RESOURCE_PREFIX = "Загрузите ресурс!";
    public static final String RESOURCE_MESSAGE_PATTERN = ADD_RESOURCE_PREFIX + " %s %s";

    private final TelegramResourceRepository resourceRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public TelegramResourceService(TelegramResourceRepository resourceRepository, ApplicationEventPublisher eventPublisher) {
        this.resourceRepository = resourceRepository;
        this.eventPublisher = eventPublisher;
    }

    public TelegramResource getResourceByName(String name) {
        return resourceRepository.findByName(editResourceName(name));
    }

    public TelegramResource initResource(TelegramResource resource) {
        String resourceName = resource.getName();
        if (resourceName == null) {
            throw new IllegalStateException("No resource name");
        }
        resourceName = editResourceName(resourceName);
        resource.setName(resourceName);
        TelegramResource existingResource = resourceRepository.findByName(resourceName);
        if (existingResource != null) {
            if (existingResource.getFileId() != null) {
                log.info("Resource {} exists. No need to load it.", resourceName);
                return existingResource;
            } else {
                resource.setId(existingResource.getId());
            }
        }
        eventPublisher.publishEvent(BotEvent.text(resource.getCreatedByChatId(),
                String.format(RESOURCE_MESSAGE_PATTERN, resource.getType().name(), resourceName)));
        return resourceRepository.save(resource);
    }

    private String editResourceName(String name) {
        return StringUtils.replace(name, " ", "-");
    }

    public boolean isResourceResponse(Message message) {
        return (message.getReplyToMessage() != null && message.getReplyToMessage().getText() != null && message.getReplyToMessage().getText().startsWith(ADD_RESOURCE_PREFIX)); /*||
                (message.getText() != null && message.getText().startsWith(ADD_RESOURCE_PREFIX));*/
    }

    public void addResource(Message message) {
        if (!isResourceResponse(message)) {
            return;
        }
        String[] infoParts = message.getReplyToMessage().getText().split(" ");
        if (infoParts.length != 4) {
            log.warn("Unable to load resource for {}", message.getReplyToMessage().getText());
            return;
        }
        TelegramResourceType type = TelegramResourceType.valueOf(infoParts[2]);
        String resourceName = infoParts[3];
        if (!type.checkMessage(message)) {
            eventPublisher.publishEvent(BotEvent.text(message.getChatId(),
                    "Похоже, вы ошиблись файлом. Это не " + type.name(), message.getMessageId()));
            return;
        }
        String fileId = type.getFileIdFromMessage(message);
        TelegramResource resource = resourceRepository.findByName(resourceName);
        if (resource == null) {
            eventPublisher.publishEvent(BotEvent.text(message.getChatId(),
                    "Что-то такого файла я не нашел " + resourceName, message.getMessageId()));
            return;
        }
        resource.setFileId(fileId);
        resourceRepository.save(resource);
        eventPublisher.publishEvent(BotEvent.text(message.getChatId(),
                "Сохранено!", message.getMessageId()));
    }
}
