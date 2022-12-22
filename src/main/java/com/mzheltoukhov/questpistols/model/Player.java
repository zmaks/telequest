package com.mzheltoukhov.questpistols.model;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.telegram.telegrambots.meta.api.objects.User;

import java.time.OffsetDateTime;
import java.util.Arrays;

@Data
public class Player {
    private Long id;
    private String username;
    private String name;
    private OffsetDateTime lastAnswer;

    public Player() {
    }

    public Player(User user) {
        this.id = user.getId();
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

    public String getMention() {
        return "[" + name + "](tg://user?id=" + id + ")";
    }
}
