package com.mzheltoukhov.questpistols.bot.model;

import com.mzheltoukhov.questpistols.model.event.bot.BotEvent;
import com.mzheltoukhov.questpistols.model.event.bot.BotEventButton;
import org.apache.commons.lang3.StringUtils;
import org.telegram.telegrambots.meta.api.methods.ForwardMessage;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.*;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public enum TelegramResourceType {
    TEXT(SendMessage.class) {

        @Override
        public PartialBotApiMethod<Message> toBotApiMethod(BotEvent event, String resourceId) {
            SendMessage method = new SendMessage();
            method.setText(event.getMessage());
            method.setChatId(String.valueOf(event.getChatId()));
            method.setReplyToMessageId(event.getReplyTo());
            method.setParseMode(ParseMode.MARKDOWN);
            if (event.getButton() != null) {
                InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton(event.getButton().getName());
                inlineKeyboardButton.setCallbackData(event.getButton().getData());
                method.setReplyMarkup(new InlineKeyboardMarkup(Collections.singletonList(Collections.singletonList(inlineKeyboardButton))));
            }
            if (event.getButtons() != null) {
                method.setReplyMarkup(TelegramResourceType.buildButtonsMarkup(event.getButtons()));
            }
            return method;
        }

        @Override
        public boolean checkMessage(Message message) {
            return message.hasText();
        }
    },
    PHOTO(SendPhoto.class) {
        @Override
        public PartialBotApiMethod<Message> toBotApiMethod(BotEvent event, String resourceId) {
            SendPhoto method = new SendPhoto();
            if (StringUtils.isNoneEmpty(event.getMessage())) {
                method.setCaption(event.getMessage());
                method.setParseMode(ParseMode.MARKDOWN);
            }
            method.setPhoto(new InputFile(resourceId));
            method.setChatId(String.valueOf(event.getChatId()));
            method.setReplyToMessageId(event.getReplyTo());
            if (event.getButtons() != null) {
                method.setReplyMarkup(TelegramResourceType.buildButtonsMarkup(event.getButtons()));
            }
            return method;
        }

        @Override
        public boolean checkMessage(Message message) {
            return message.hasPhoto();
        }

        @Override
        public String getFileIdFromMessage(Message message) {
            return message.getPhoto().get(0).getFileId();
        }
    },
    VIDEO(SendVideo.class) {
        @Override
        public PartialBotApiMethod<Message> toBotApiMethod(BotEvent event, String resourceId) {
            SendVideo method = new SendVideo();
            if (StringUtils.isNoneEmpty(event.getMessage())) {
                method.setCaption(event.getMessage());
            }
            method.setVideo(new InputFile(resourceId));

            method.setChatId(String.valueOf(event.getChatId()));
            method.setReplyToMessageId(event.getReplyTo());
            if (event.getButtons() != null) {
                method.setReplyMarkup(TelegramResourceType.buildButtonsMarkup(event.getButtons()));
            }
            return method;
        }

        @Override
        public boolean checkMessage(Message message) {
            return message.hasVideo();
        }

        @Override
        public String getFileIdFromMessage(Message message) {
            return message.getVideo().getFileId();
        }
    },
    AUDIO(SendAudio.class) {
        @Override
        public PartialBotApiMethod<Message> toBotApiMethod(BotEvent event, String resourceId) {
            SendAudio method = new SendAudio();
            if (StringUtils.isNoneEmpty(event.getMessage())) {
                method.setCaption(event.getMessage());
            }
            method.setAudio(new InputFile(resourceId));

            method.setChatId(String.valueOf(event.getChatId()));
            method.setReplyToMessageId(event.getReplyTo());
            if (event.getButtons() != null) {
                method.setReplyMarkup(TelegramResourceType.buildButtonsMarkup(event.getButtons()));
            }
            return method;
        }

        @Override
        public boolean checkMessage(Message message) {
            return message.hasAudio();
        }

        @Override
        public String getFileIdFromMessage(Message message) {
            return message.getAudio().getFileId();
        }
    },
    DOCUMENT(SendDocument.class) {
        @Override
        public PartialBotApiMethod<Message> toBotApiMethod(BotEvent event, String resourceId) {
            SendDocument method = new SendDocument();
            if (StringUtils.isNoneEmpty(event.getMessage())) {
                method.setCaption(event.getMessage());
            }
            method.setDocument(new InputFile(resourceId));

            method.setChatId(String.valueOf(event.getChatId()));
            method.setReplyToMessageId(event.getReplyTo());
            return method;
        }

        @Override
        public boolean checkMessage(Message message) {
            return message.hasDocument();
        }

        @Override
        public String getFileIdFromMessage(Message message) {
            return message.getDocument().getFileId();
        }
    },
    ANIMATION(SendAnimation.class) {
        @Override
        public PartialBotApiMethod<Message> toBotApiMethod(BotEvent event, String resourceId) {
            SendAnimation method = new SendAnimation();
            if (StringUtils.isNoneEmpty(event.getMessage())) {
                method.setCaption(event.getMessage());
            }
            method.setAnimation(new InputFile(resourceId));

            method.setChatId(String.valueOf(event.getChatId()));
            method.setReplyToMessageId(event.getReplyTo());
            return method;
        }

        @Override
        public boolean checkMessage(Message message) {
            return message.hasAnimation();
        }

        @Override
        public String getFileIdFromMessage(Message message) {
            return message.getAnimation().getFileId();
        }
    },
    VOICE(SendVoice.class) {
        @Override
        public PartialBotApiMethod<Message> toBotApiMethod(BotEvent event, String resourceId) {
            SendVoice method = new SendVoice();
            if (StringUtils.isNoneEmpty(event.getMessage())) {
                method.setCaption(event.getMessage());
            }
            method.setVoice(new InputFile(resourceId));

            method.setChatId(String.valueOf(event.getChatId()));
            method.setReplyToMessageId(event.getReplyTo());
            return method;
        }

        @Override
        public boolean checkMessage(Message message) {
            return message.hasVoice();
        }

        @Override
        public String getFileIdFromMessage(Message message) {
            return message.getVoice().getFileId();
        }
    },
    STICKER(SendSticker.class) {
        @Override
        public PartialBotApiMethod<Message> toBotApiMethod(BotEvent event, String resourceId) {
            SendSticker method = new SendSticker();
            method.setSticker(new InputFile(resourceId));
            method.setChatId(String.valueOf(event.getChatId()));
            method.setReplyToMessageId(event.getReplyTo());
            return method;
        }

        @Override
        public boolean checkMessage(Message message) {
            return message.hasSticker();
        }

        @Override
        public String getFileIdFromMessage(Message message) {
            return message.getSticker().getFileId();
        }
    };

    private static List<String> values;

    static {
        values = Arrays.stream(TelegramResourceType.values()).map(Enum::name).collect(Collectors.toList());
    }

    public static List<String> getValues() {
        return values;
    }

    private Class<?> methodClass;

    TelegramResourceType(Class<?> methodClass) {
        this.methodClass = methodClass;
    }

    public <T> T toBotApiMethod(BotEvent event, String resourceId) {
        throw new UnsupportedOperationException("Not supported for type " + this.toString());
    }

    public boolean checkMessage(Message message) {
        throw new UnsupportedOperationException("Not supported for type " + this.toString());
    }

    public String getFileIdFromMessage(Message message) {
        throw new UnsupportedOperationException("Not supported for type " + this.toString());
    }

    private static InlineKeyboardMarkup buildButtonsMarkup(List<BotEventButton> botEventButtons) {
        List<List<InlineKeyboardButton>> buttons = botEventButtons.stream()
                .map(b -> {
                    InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton(b.getName());
                    inlineKeyboardButton.setCallbackData(b.getData());
                    return Collections.singletonList(inlineKeyboardButton);
                })
                .collect(Collectors.toList());
        return new InlineKeyboardMarkup(buttons);
    }
}
