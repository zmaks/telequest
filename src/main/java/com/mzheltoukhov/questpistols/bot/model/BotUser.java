package com.mzheltoukhov.questpistols.bot.model;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.telegram.telegrambots.meta.api.objects.User;

import java.time.OffsetDateTime;
import java.util.Arrays;

@Document("users")
@Data
public class BotUser {
    @Id
    private String id;
    private Long userId;
    private String username;
    private String name;
    private String lastMessage;
    private OffsetDateTime lastMessageAt;
    private String registeredGame;
    private OffsetDateTime registeredAt;
    private String errorMessage;
    private Integer inviteMessageId;
    private int attempts;
    private int registrationCancellations;

    public BotUser() {
    }

    public BotUser(User user) {
        this.userId = user.getId();
        if (StringUtils.isNoneBlank(user.getUserName())) {
            this.username = user.getUserName().replace("@", "");
        }
        this.name = getName(user);
    }

    private String getName(User user) {
        String name;
        String firstName = StringUtils.defaultString(user.getFirstName());
        String lastName = StringUtils.defaultString(user.getLastName());
        name = String.join(" ", Arrays.asList(firstName, lastName)).trim();
        if (StringUtils.isBlank(name)) {
            name = user.getUserName();
        }
        return name;
    }
}
