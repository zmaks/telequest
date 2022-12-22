package com.mzheltoukhov.questpistols.bot;

import com.mzheltoukhov.questpistols.bot.handler.GroupMessageHandler;
import com.mzheltoukhov.questpistols.bot.handler.UserMessageHandler;
import com.mzheltoukhov.questpistols.bot.model.RegistrationResult;
import com.mzheltoukhov.questpistols.bot.model.TelegramResource;
import com.mzheltoukhov.questpistols.bot.model.TelegramResourceType;
import com.mzheltoukhov.questpistols.bot.service.TelegramResourceService;
import com.mzheltoukhov.questpistols.bot.service.UserService;
import com.mzheltoukhov.questpistols.configuration.telegram.TelegramBotProperties;
import com.mzheltoukhov.questpistols.model.GameMembersChangedMessage;
import com.mzheltoukhov.questpistols.model.PayloadType;
import com.mzheltoukhov.questpistols.model.Player;
import com.mzheltoukhov.questpistols.model.event.bot.*;
import com.mzheltoukhov.questpistols.service.GameService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.util.CollectionUtils;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.*;
import org.telegram.telegrambots.meta.api.methods.groupadministration.*;
import org.telegram.telegrambots.meta.api.methods.pinnedmessages.PinChatMessage;
import org.telegram.telegrambots.meta.api.methods.pinnedmessages.UnpinChatMessage;
import org.telegram.telegrambots.meta.api.methods.send.*;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaDocument;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaVideo;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
public class QuestPistolsBot extends TelegramLongPollingBot {

    private final TelegramBotProperties botProperties;

    @Value("${bot.player-registration-mode:false}")
    private boolean playerRegistrationMode;

    @Value("#{'${bot.admins:z_maks}'.split(',')}")
    private List<String> admins;

    @Autowired
    private GroupMessageHandler groupMessageHandler;

    @Autowired
    private UserMessageHandler userMessageHandler;

    @Autowired
    private TelegramResourceService resourceService;

    @Autowired
    private GameService gameService;

    @Autowired
    private UserService userService;

    public QuestPistolsBot(DefaultBotOptions options, TelegramBotProperties properties) {
        super(options);
        this.botProperties = properties;
    }

    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage()) {

            if (update.getMessage().getMigrateToChatId() != null) {
                gameService.updateGameChatId(update.getMessage().getMigrateFromChatId(), update.getMessage().getMigrateToChatId());
            }
            if (!CollectionUtils.isEmpty(update.getMessage().getNewChatMembers())) {
                onNewChatMembers(update);
                return;
            }
            if (update.getMessage().getLeftChatMember() != null) {
                onChatMembersLeft(update);
                return;
            }
            if (update.getMessage().getChat().isGroupChat() || update.getMessage().getChat().isSuperGroupChat()) {
                groupMessageHandler.handle(update.getMessage());
            } else {
                if (playerRegistrationMode && !admins.contains(update.getMessage().getFrom().getUserName())) {
                    Message message = update.getMessage();
                    registerUser(message);
                } else {
                    userMessageHandler.handle(update.getMessage());
                }
            }

            return;
        }
        if (update.hasCallbackQuery()) {
            userMessageHandler.handleCallback(update.getCallbackQuery());
        }
    }

    private synchronized void registerUser(Message message) {
        RegistrationResult registrationResult = userService.register(message);
        if (registrationResult.isRegistered()) {
            CreateChatInviteLink linkMethod = CreateChatInviteLink.builder()
                    .chatId(String.valueOf(registrationResult.getInvitedToChatId()))
                    .memberLimit(1)
                    .name(registrationResult.getTargetUser())
                    .build();
            try {
                ChatInviteLink chatInviteLink = sendApiMethod(linkMethod);
                SendMessage sendMessage = SendMessage.builder()
                        .chatId(String.valueOf(message.getChatId()))
                        .text( String.format("%s\n\n%s",
                                registrationResult.getMessage(),
                                registrationResult.getRegistrationCodeMessage()))
                        .replyMarkup(
                                InlineKeyboardMarkup.builder().keyboardRow(List.of(
                                    InlineKeyboardButton.builder().text("Войти в игру").url(chatInviteLink.getInviteLink()).build()
                                )).build()
                        ).parseMode(ParseMode.MARKDOWN)
                        .build();
                Message inviteResultMessage = sendApiMethod(sendMessage);
                userService.setInviteMessageId(registrationResult.getTargetUserId(), inviteResultMessage.getMessageId());
            } catch (TelegramApiException e) {
                log.error("Unable to create chat invite link", e);
                onBotEvent(BotEvent.text(message.getChatId(), "Ошибка. Попробуйте еще раз или обратитесь к администратору"));
                gameService.cancelInvite(registrationResult.getInvitedToChatId());
                userService.cancelRegistration(message.getFrom().getId(), false);
            }
            sendSilentAdminNotification(String.format("Игрок %s -> %s", registrationResult.getTargetUser(), registrationResult.getGameName()));
        } else {
            onBotEvent(BotEvent.text(message.getChatId(), registrationResult.getMessage()));
        }
    }

    private void deleteInviteLinkButton(Long userChatWithBotId) {
        Integer messageId = userService.getInviteMessageId(userChatWithBotId);
        if (messageId == null) {
            log.info("Invite link button message ID was not found for user {}", userChatWithBotId);
            return;
        }
        EditMessageReplyMarkup editMessageReplyMarkup = EditMessageReplyMarkup.builder()
                .messageId(messageId)
                .chatId(String.valueOf(userChatWithBotId))
                .replyMarkup(InlineKeyboardMarkup.builder().clearKeyboard().build())
                .build();
        try {
            sendApiMethod(editMessageReplyMarkup);
        } catch (TelegramApiException e) {
            log.error("Unable to delete chat invite link button", e);
        }
    }

    private void onChatMembersLeft(Update update) {
        var getChatMembersCount = new GetChatMemberCount();
        getChatMembersCount.setChatId(String.valueOf(update.getMessage().getChatId()));
        try {
            Integer count = sendApiMethod(getChatMembersCount);
            if (!count.equals(0)) {
                groupMessageHandler.handleLeftMembers(new GameMembersChangedMessage(
                        update.getMessage().getChatId(), count,
                        List.of(new Player(update.getMessage().getLeftChatMember())))
                );
            }
            if (playerRegistrationMode && update.getMessage().getLeftChatMember() != null
                    && update.getMessage().getChat() != null && !admins.contains(update.getMessage().getLeftChatMember().getUserName())) {
                log.info("User {} left chat {}. Canceling registration", update.getMessage().getLeftChatMember().getId(), update.getMessage().getChat().getTitle());
                gameService.cancelInvite(update.getMessage().getChatId());
                userService.cancelRegistration(update.getMessage().getLeftChatMember().getId(), true);
                Player player = new Player(update.getMessage().getLeftChatMember());
                sendSilentAdminNotification(String.format("Игрок %s X %s", player.getName(), update.getMessage().getChat().getTitle()));
            }
        } catch (TelegramApiException e) {
            log.error("Cannot get members count", e);
        }
    }

    private void onNewChatMembers(Update update) {
        var getChatMembersCount = new GetChatMembersCount();
        getChatMembersCount.setChatId(String.valueOf(update.getMessage().getChatId()));
        try {
            Integer count = sendApiMethod(getChatMembersCount);
            if (!count.equals(0)) {
                groupMessageHandler.handleNewMembers(new GameMembersChangedMessage(
                        update.getMessage().getChatId(), count,
                        update.getMessage().getNewChatMembers().stream()
                                .filter(u -> !u.getIsBot()).map(Player::new).collect(Collectors.toList())));
            }
            if (playerRegistrationMode && update.getMessage().getNewChatMembers() != null &&
                    !update.getMessage().getNewChatMembers().isEmpty()) {
                User addedUser = update.getMessage().getNewChatMembers().get(0);
                if (update.getMessage().getChat() != null && !admins.contains(addedUser.getUserName())) {
                    log.info("User {} entered the game chat {}. Removing invite button.", addedUser.getId(), update.getMessage().getChat().getTitle());
                    deleteInviteLinkButton(addedUser.getId());
                    onBotEvent(BotEvent.text(update.getMessage().getChatId(), "\uD83D\uDC4B"));
                }
            }
        } catch (TelegramApiException e) {
            log.error("Cannot get members count", e);
        }
    }

    private void sendSilentAdminNotification(String message) {
        try {
            SendMessage sendMessage = SendMessage.builder().text(message).disableNotification(true).chatId("183375382").build();
            sendApiMethod(sendMessage);
        } catch (Exception e) {
            log.error("Cannot send silent admin message", e);
        }

    }

    @EventListener
    public void onForwardedMessageBotEvent(ForwardedMessageBotEvent event) {
            BotEvent botEvent = BotEvent.text(event.getForwardToChatId(), event.getComment());
            botEvent.setPayloadType(event.getPayload().getType());
            botEvent.setFileId(event.getPayload().getFileId());
            onBotEvent(botEvent);
    }

    @EventListener
    public void onForwardedBotEvent(ForwardedToChatBotEvent event) {
        CopyMessage copyMessage = CopyMessage.builder()
                .fromChatId(String.valueOf(event.getFromChatId()))
                .chatId(String.valueOf(event.getForwardToChatId()))
                .messageId(event.getForwardedMessageId())
                .build();
        try {
            sendApiMethod(copyMessage);
        } catch (TelegramApiException e) {
            if (e instanceof TelegramApiRequestException) {
                TelegramApiRequestException ex = (TelegramApiRequestException) e;
                if (ex.getParameters() != null && ex.getParameters().getMigrateToChatId() != null) {
                    Long migratedChatId = ex.getParameters().getMigrateToChatId();
                    gameService.updateGameChatId(event.getForwardToChatId(), migratedChatId);
                }
                log.error("Unable to handle ForwardedToChatBotEvent {}\n{}. Params: {}",
                        event, ex.getApiResponse(), ex.getParameters());
            } else {
                log.error("Unable to handle ForwardedToChatBotEvent {}\n{}", event, e.getMessage(), e);
            }
        }
    }

    @EventListener
    public void onSendFileEvent(SendFileEvent event) throws IOException {
        SendDocument sendDocument = new SendDocument();
        sendDocument.setChatId(String.valueOf(event.getChatId()));
        sendDocument.setReplyToMessageId(event.getReplyTo());
        PipedInputStream in = new PipedInputStream();
        final PipedOutputStream out = new PipedOutputStream(in);
        new Thread(() -> {
            try (out; var byteOs = event.getStream()) {
                byteOs.writeTo(out);
            } catch (IOException e) {
                log.error("Unable to send doc", e);
            }
        }).start();
        sendDocument.setDocument(new InputFile(in, event.getFileName()));
        try {
            execute(sendDocument);
        } catch (TelegramApiException e) {
            log.error("Unable to send doc", e);
        }
    }

    public List<InputMedia> convertGroupMedia(List<BotEventPayload> payloadGroup) {
        List<InputMedia> medias = new ArrayList<>();
        for(BotEventPayload payload : payloadGroup) {
            String fileId = getFileId(payload.getResourceName());
            if (fileId == null) continue;
            if (payload.getPayloadType() == PayloadType.PHOTO) {
                medias.add(new InputMediaPhoto(fileId));
            }
            if (payload.getPayloadType() == PayloadType.VIDEO) {
                medias.add(new InputMediaVideo(fileId));
            }
            if (payload.getPayloadType() == PayloadType.DOCUMENT) {
                medias.add(new InputMediaDocument(fileId));
            }
        }
        return medias;
    }

    @EventListener
    public void onBotEvent(BotEvent event) {
        Message response = null;
        int attempts = 0;
        while (attempts < 3) {
            try {
                log.info("{}| Handling bot event: {}", event.getChatId(), event);
                if (!CollectionUtils.isEmpty(event.getPayloadGroup())) {
                    List<InputMedia> media = convertGroupMedia(event.getPayloadGroup());
                    if (!CollectionUtils.isEmpty(media)) {
                        SendMediaGroup sendMediaGroup = new SendMediaGroup(String.valueOf(event.getChatId()), media);
                        execute(sendMediaGroup);
                    }
                } else {
                    TelegramResourceType type = event.getPayloadType() == null ? TelegramResourceType.TEXT :
                            TelegramResourceType.valueOf(event.getPayloadType().name());
                    String fileId = event.getFileId() != null ? event.getFileId() : getFileId(event.getResourceName());
                    PartialBotApiMethod<Message> method = type.toBotApiMethod(event, fileId);
                    Thread.sleep(new Random().nextInt(150));
                    switch (type) {
                        case TEXT:
                            if (StringUtils.isNoneEmpty(event.getMessage())) {
                                response = sendApiMethod((SendMessage) method);
                            }
                            break;
                        case PHOTO:
                            response = execute((SendPhoto) method);
                            break;
                        case VIDEO:
                            response = execute((SendVideo) method);
                            break;
                        case AUDIO:
                            response = execute((SendAudio) method);
                            break;
                        case VOICE:
                            response = execute((SendVoice) method);
                            break;
                        case DOCUMENT:
                            response = execute((SendDocument) method);
                            break;
                        case STICKER:
                            response = execute((SendSticker) method);
                            break;
                        case ANIMATION:
                            response = execute((SendAnimation) method);
                            break;
                    }
                }
                try {
                    if (event.isPin() && response != null) {
                        Thread.sleep(100);
                        PinChatMessage pinChatMessage =
                                new PinChatMessage(String.valueOf(event.getChatId()), response.getMessageId());
                        sendApiMethod(pinChatMessage);
                    }
                    if (event.isUnPin()) {
                        Thread.sleep(100);
                        UnpinChatMessage unpinChatMessage = new UnpinChatMessage(String.valueOf(event.getChatId()));
                        sendApiMethod(unpinChatMessage);
                    }
                } catch (TelegramApiException e) {
                    log.error("Unable to handle BotEvent {}\n{}.", event, e.getMessage());
                }
                break;
            } catch (Exception e) {
                attempts++;
                if (e instanceof TelegramApiRequestException) {
                    TelegramApiRequestException ex = (TelegramApiRequestException) e;
                    if (ex.getParameters() != null && ex.getParameters().getMigrateToChatId() != null) {
                        Long migratedChatId = ex.getParameters().getMigrateToChatId();
                        gameService.updateGameChatId(event.getChatId(), migratedChatId);
                        event.setChatId(migratedChatId);
                    }
                    if (ex.getParameters() != null && ex.getErrorCode().equals(429) && ex.getParameters().getRetryAfter() != null) {
                        try {
                            log.error("TOO MANY REQUESTS. Pause chat {}. Will retry after {}s",
                                    event.getChatId(), ex.getParameters().getRetryAfter());
                            Thread.sleep(ex.getParameters().getRetryAfter() * 1000);
                        } catch (InterruptedException interruptedException) {
                            Thread.currentThread().interrupt();
                            log.error(interruptedException.getMessage(), interruptedException);
                        }
                        continue;
                    }
                    log.error("Unable to handle BotEvent {}\n{}. Params: {}. Attempt: {}",
                            event, ex.getApiResponse(), ex.getParameters(), attempts);
                } else {
                    log.error("Unable to handle BotEvent {}\n{}", event, e.getMessage(), e);
                }
                try {
                    log.error(e.getMessage(), e);
                    Thread.sleep(500 + new Random().nextInt(1000));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    @EventListener
    public void onGetFileEvent(DownloadFileEvent event) {
        try {
            GetFile getFile = new GetFile();
            getFile.setFileId(event.getFileId());
            File file = sendApiMethod(getFile);
            InputStream is = downloadFileAsStream(file.getFilePath());
            event.getFileConsumer().accept(is);
        } catch (TelegramApiRequestException e) {
            log.error("Unable to handle DownloadFileEvent {}\n{}", event, e.getApiResponse(), e);
        } catch (Exception e) {
            log.error("Unable to handle DownloadFileEvent {}", event, e);
        }
    }

    private String getFileId(String resourceName) {
        if (resourceName == null) {
            return null;
        }
        TelegramResource resource = resourceService.getResourceByName(resourceName);
        if (resource == null) {
            throw new IllegalStateException("Resource not found: " + resourceName);
        }
        if (resource.getFileId() == null) {
            return resourceName;
        }
        return resource.getFileId();
    }

    @Override
    public String getBotUsername() {
        return botProperties.getBotName();
    }

    @Override
    public String getBotToken() {
        return botProperties.getToken();
    }
}
