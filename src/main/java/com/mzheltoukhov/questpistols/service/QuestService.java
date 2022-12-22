package com.mzheltoukhov.questpistols.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.mzheltoukhov.questpistols.bot.model.TelegramResource;
import com.mzheltoukhov.questpistols.bot.model.TelegramResourceType;
import com.mzheltoukhov.questpistols.bot.service.TelegramResourceService;
import com.mzheltoukhov.questpistols.exception.DefaultQuestNotFoundException;
import com.mzheltoukhov.questpistols.model.Quest;
import com.mzheltoukhov.questpistols.model.event.bot.BotEvent;
import com.mzheltoukhov.questpistols.model.task.QuestTask;
import com.mzheltoukhov.questpistols.repository.QuestRepository;
import com.mzheltoukhov.questpistols.repository.QuestTaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class QuestService {

    @Autowired
    private QuestRepository questRepository;

    @Autowired
    private QuestTaskRepository questTaskRepository;

    @Autowired
    private TelegramResourceService resourceService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private ObjectMapper mapper;

    @Value("${quest.default.name}")
    protected String defaultQuestName;

    public Quest getDemoQuest() {
        return findByKeyWord("demo");
    }

    public Quest getDefaultQuest() throws DefaultQuestNotFoundException {
        if (defaultQuestName == null) {
            throw new DefaultQuestNotFoundException("Name is not specified");
        }
        Quest quest = questRepository.findByName(defaultQuestName);
        if (quest == null) {
            throw new DefaultQuestNotFoundException(defaultQuestName + " - quest not found");
        }
        return quest;
    }

    public Quest findByName(String name) {
        return questRepository.findByName(name);
    }

    public Quest findByKeyWord(String keyWord) {
        List<Quest> quests = questRepository.findByKeyWordsContains(keyWord);
        if (quests.isEmpty()) {
            return null;
        }
        return quests.get(0);
    }

    public void loadQuest(String questJson, Long creatorChatId) {
        Quest parsedQuest;
        try {
            parsedQuest = mapper.readValue(questJson, Quest.class);
        } catch (JsonProcessingException e) {
            eventPublisher.publishEvent(BotEvent.text(creatorChatId, "Unable to parse quest json"));
            log.error(e.getMessage(), e);
            return;
        }
        if (parsedQuest.getName() == null) {
            eventPublisher.publishEvent(BotEvent.text(creatorChatId, "Quest *name* is required"));
            return;
        }
        if (parsedQuest.getTasks().stream().anyMatch(task -> StringUtils.isEmpty(task.getName()))) {
            eventPublisher.publishEvent(BotEvent.text(creatorChatId, "Quest task *name* is required for all tasks"));
            return;
        }
        if (parsedQuest.getTasks().stream().anyMatch(task -> task.getType() == null)) {
            eventPublisher.publishEvent(BotEvent.text(creatorChatId, "Quest task *type* is required for all tasks"));
            return;
        }


        Quest existingQuest = questRepository.findByName(parsedQuest.getName());
        if (existingQuest != null) {
            parsedQuest.setId(existingQuest.getId());
        }
        parsedQuest.setCreatedByChatId(creatorChatId);
        parsedQuest.setCreatedTime(OffsetDateTime.now());
        final String questName = parsedQuest.getName();
        parsedQuest.setTasks(
                parsedQuest.getTasks().stream().map(t -> saveTask(t, questName)).collect(Collectors.toList())
        );
        parsedQuest = questRepository.save(parsedQuest);
        initAllResources(parsedQuest);

//        for (QuestTask questTask : parsedQuest.getTasks()) {
//            if (questTask.getPlug() != null && questTask.getPlug().getPayload() != null) {
//                resourceService.initResource(TelegramResource.builder()
//                        .name(questTask.getPlug().getPayload().getResourceName())
//                        .createdByChatId(creatorChatId)
//                        .createdTime(OffsetDateTime.now())
//                        .type(TelegramResourceType.valueOf(questTask.getPlug().getPayload().getType().name())).build());
//            }
//            for (ResponsePayload responsePayload : questTask.getDescriptions()) {
//                if (StringUtils.isNoneEmpty(responsePayload.getResourceName())) {
//                    resourceService.initResource(TelegramResource.builder()
//                            .name(responsePayload.getResourceName())
//                            .createdByChatId(creatorChatId)
//                            .createdTime(OffsetDateTime.now())
//                            .type(TelegramResourceType.valueOf(responsePayload.getType().name())).build());
//                }
//            }
//        }
        eventPublisher.publishEvent(BotEvent.text(creatorChatId, "Квест успешно загружен!"));
        log.info("Quest has been successfully loaded: {}", parsedQuest.getName());
    }

    private void initAllResources(Quest quest) {
        try {
            JsonNode questNode = mapper.readTree(mapper.writeValueAsString(quest));
            Set<TelegramResource> resources = new LinkedHashSet<>();
            OffsetDateTime createdTime = OffsetDateTime.now();
            parseResource(questNode, resources, createdTime, quest.getCreatedByChatId());
            for (TelegramResource resource : resources) {
                resourceService.initResource(resource);
            }
        } catch (JsonProcessingException e) {
            log.error("Quest parsing error", e);
        }
    }

    private void parseResource(JsonNode node, Set<TelegramResource> resources, OffsetDateTime createdTime, Long chatId) {
        if (!node.isObject() && !node.isArray()) {
            return;
        }
        TelegramResource resource = new TelegramResource();
        try {
            node.fields().forEachRemaining(entry -> {
                try {
                    if (entry.getValue().isArray()) {
                        ((ArrayNode) entry.getValue()).forEach(jsonNode -> parseResource(jsonNode, resources, createdTime, chatId));
                        return;
                    }
                    if (entry.getValue().isObject()) {
                        parseResource(entry.getValue(), resources, createdTime, chatId);
                        return;
                    }
                    if (entry.getKey().equals("resourceName")) {
                        resource.setName(entry.getValue().asText());
                        return;
                    }
                    if (entry.getKey().equals("type") && !entry.getValue().asText().equals("TEXT")
                             && TelegramResourceType.getValues().contains(entry.getValue().asText())) {
                        resource.setType(TelegramResourceType.valueOf(entry.getValue().asText()));
                    }
                } catch (Exception e) {
                    log.error("Quest parsing error", e);
                }
            });
        } catch (Exception e) {
            log.error("Quest parsing error", e);
        }
        if (resource.getName() != null && resource.getType() != null) {
            resource.setCreatedTime(createdTime);
            resource.setCreatedByChatId(chatId);
            resources.add(resource);
        }
    }

    private QuestTask saveTask(QuestTask questTask, String questName) {
        QuestTask existing = questTaskRepository.findByNameAndQuestName(questTask.getName(), questName);
        if (existing != null) {
            questTask.setId(existing.getId());
        }
        questTask.setQuestName(questName);
        for (int i = 0; i < questTask.getAnswers().size(); i++) {
            questTask.getAnswers().get(i).setId(i + 1);
            if (!questTask.getAnswers().get(i).isStrict() && !CollectionUtils.isEmpty(questTask.getAnswers().get(i).getText())) {
                questTask.getAnswers().get(i).setText(
                    questTask.getAnswers().get(i).getText().stream()
                            .map(String::toLowerCase)
                            .map(StringUtils::deleteWhitespace)
                            .map(s -> s.replace("ё", "е"))
                            .collect(Collectors.toList())
                );
            }
        }
        return questTaskRepository.save(questTask);
    }

}
